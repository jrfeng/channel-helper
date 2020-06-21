package channel.processor;

import channel.helper.Channel;
import channel.helper.Dispatcher;
import channel.helper.Emitter;
import channel.helper.ParamInspector;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import channel.helper.UseOrdinal;
import javafx.util.Pair;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
        "channel.helper.Channel"
})
public class ChannelProcessor extends AbstractProcessor {
    private Types mTypes;
    private Messager mMessager;
    private Elements mElements;

    private static final String PREFIX_METHOD_ID = "METHOD_ID_";
    private static final String FIELD_KEY_CLASS_NAME = "KEY_CLASS_NAME";
    private static final String FIELD_KEY_METHOD_ID = "KEY_METHOD_ID";
    private static final String FIELD_CLASS_NAME = "CLASS_NAME";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mTypes = processingEnv.getTypeUtils();
        mMessager = processingEnv.getMessager();
        mElements = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<TypeElement> interfaces = getAnnotatedInterfaces(roundEnvironment);
        for (TypeElement element : interfaces) {
            buildChannelHelper(element);
        }
        return true;
    }

    private Set<TypeElement> getAnnotatedInterfaces(RoundEnvironment roundEnvironment) {
        Set<TypeElement> interfaces = new HashSet<>();

        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Channel.class);
        for (Element element : elements) {
            if (isInterface(element)) {
                interfaces.add((TypeElement) element);
            }
        }

        return interfaces;
    }

    private boolean isInterface(Element element) {
        return element.getKind() == ElementKind.INTERFACE;
    }

    private void buildChannelHelper(TypeElement targetInterface) {
        Channel channel = targetInterface.getAnnotation(Channel.class);

        List<ExecutableElement> methods = getAllMethod(targetInterface);

        ParamInspector inspector = getInspector(channel.inspector());
        if (inspector != null) {
            checkMethodsParamType(methods, inspector, targetInterface);
        }

        List<Pair<String, ExecutableElement>> methodIdPairs = generateAllMethodId(methods);

        TypeSpec channelWrapperType = buildChannelWrapper(targetInterface, methodIdPairs);

        writeJavaFile(channelWrapperType, targetInterface);
    }

    private void writeJavaFile(TypeSpec typeSpec, TypeElement targetInterface) {
        JavaFile javaFile = JavaFile.builder(mElements.getPackageOf(targetInterface).getQualifiedName().toString(), typeSpec)
                .build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, e.toString());
        }
    }

    private List<ExecutableElement> getAllMethod(TypeElement element) {
        List<ExecutableElement> result = new ArrayList<>();

        List<? extends TypeMirror> superTypes = element.getInterfaces();

        for (TypeMirror superType : superTypes) {
            result.addAll(getAllMethod((TypeElement) mTypes.asElement(superType)));
        }

        List<? extends Element> elements = element.getEnclosedElements();

        for (Element e : elements) {
            if (e instanceof ExecutableElement) {
                result.add((ExecutableElement) e);
            }
        }

        return result;
    }

    private ParamInspector getInspector(String inspectorName) {
        if ("".equals(inspectorName)) {
            return null;
        }

        try {
            return (ParamInspector) Class.forName(inspectorName).newInstance();
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, e.toString());
        }

        return null;
    }

    private void checkMethodsParamType(List<ExecutableElement> methods, ParamInspector inspector, TypeElement targetInterface) {
        for (ExecutableElement method : methods) {
            checkParamType(method, inspector, targetInterface);
        }
    }

    private void checkParamType(ExecutableElement method, ParamInspector inspector, TypeElement targetInterface) {
        List<? extends VariableElement> params = method.getParameters();
        for (VariableElement param : params) {
            if (inspector.isIllegal(param)) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "Illegal parameter type:\n" +
                        "    interface : " + targetInterface.getQualifiedName() + "\n" +
                        "    method    : " + method.getSimpleName() + "\n" +
                        "    param     : " + param.toString() + "\n" +
                        "    param type: " + param.asType().toString());
            }
        }
    }

    private List<Pair<String, ExecutableElement>> generateAllMethodId(List<ExecutableElement> methods) {
        List<Pair<String, ExecutableElement>> methodIdPairs = new ArrayList<>(methods.size());

        for (int i = 1; i <= methods.size(); i++) {
            ExecutableElement method = methods.get(i - 1);
            methodIdPairs.add(new Pair<>(PREFIX_METHOD_ID + i, method));
        }

        return methodIdPairs;
    }

    private TypeSpec buildChannelWrapper(TypeElement targetInterface, List<Pair<String, ExecutableElement>> methodIdPairs) {
        String wrapperName = getChannelWrapperName(targetInterface) + "__ChannelWrapper";

        ClassName string = ClassName.get("java.lang", "String");
        FieldSpec KEY_CLASS_NAME = FieldSpec.builder(string, FIELD_KEY_CLASS_NAME, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", "__class_name")
                .build();
        FieldSpec KEY_METHOD_ID = FieldSpec.builder(string, FIELD_KEY_METHOD_ID, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", "__method_id")
                .build();
        FieldSpec CLASS_NAME = FieldSpec.builder(string, FIELD_CLASS_NAME, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", targetInterface.getQualifiedName())
                .build();

        // private default constructor
        MethodSpec defaultConstructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE)
                .addStatement("throw new AssertionError()")
                .build();

        TypeSpec.Builder builder = TypeSpec.classBuilder(wrapperName)
                .addMethod(defaultConstructor)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(KEY_CLASS_NAME)
                .addField(KEY_METHOD_ID)
                .addField(CLASS_NAME)
                .addType(buildEmitter(targetInterface, methodIdPairs))
                .addType(buildDispatcher(targetInterface, methodIdPairs))
                .addFields(generateMethodIdField(methodIdPairs));

        return builder.build();
    }

    private String getChannelWrapperName(TypeElement targetInterface) {
        String packageName = mElements.getPackageOf(targetInterface).getQualifiedName().toString();
        String qualifiedName = targetInterface.getQualifiedName().toString();

        return qualifiedName.substring(packageName.length() + 1).replace('.', '$');
    }

    private List<FieldSpec> generateMethodIdField(List<Pair<String, ExecutableElement>> methodIdPairs) {
        List<FieldSpec> methodIdFieldList = new ArrayList<>(methodIdPairs.size());

        int i = 1;
        for (Pair<String, ExecutableElement> methodIdPair : methodIdPairs) {
            methodIdFieldList.add(
                    FieldSpec.builder(TypeName.INT, methodIdPair.getKey(), Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$L", i)
                            .build()
            );
            i++;
        }

        return methodIdFieldList;
    }

    private TypeSpec buildEmitter(TypeElement targetInterface, List<Pair<String, ExecutableElement>> methodIdPairs) {
        TypeSpec.Builder builder = TypeSpec.classBuilder("Emitter")
                .addSuperinterface(targetInterface.asType())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

        ClassName Emitter = ClassName.get(Emitter.class);

        // field
        final String field_emitter = "emitter";

        FieldSpec emitter = FieldSpec.builder(Emitter, field_emitter, Modifier.PRIVATE)
                .build();

        builder.addField(emitter);

        // constructor
        final String param_emitter = "emitter";

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Emitter, param_emitter)
                .addStatement("this.$N = $N", field_emitter, param_emitter)
                .build();

        builder.addMethod(constructor);

        // method: private void sendMessage(int id, Map<String, Object> args)
        final String method_sendMessage = "sendMessage";
        final String param_id = "id";
        final String param_args = "args";

        // Map<String, Object>
        ParameterizedTypeName type_args = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(Object.class)
        );

        MethodSpec sendMessage = MethodSpec.methodBuilder(method_sendMessage)
                .addModifiers(Modifier.PRIVATE)
                .returns(TypeName.VOID)
                .addParameter(TypeName.INT, param_id)
                .addParameter(type_args, param_args)
                .addStatement("$N.put($N, $N)", param_args, FIELD_KEY_CLASS_NAME, FIELD_CLASS_NAME)
                .addStatement("$N.put($N, $N)", param_args, FIELD_KEY_METHOD_ID, param_id)
                .addStatement("$N.emit($N)", field_emitter, param_args)
                .build();

        builder.addMethod(sendMessage);

        // override targetInterface
        for (Pair<String, ExecutableElement> methodPair : methodIdPairs) {
            builder.addMethod(
                    overrideEmitterMethod(methodPair)
            );
        }

        return builder.build();
    }

    private MethodSpec overrideEmitterMethod(Pair<String, ExecutableElement> methodPair) {
        final String methodId = methodPair.getKey();
        final ExecutableElement method = methodPair.getValue();

        // Map<String, Object>
        ParameterizedTypeName type_args = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(Object.class)
        );

        final String variable_args = "args";
        MethodSpec.Builder builder = MethodSpec.overriding(methodPair.getValue())
                .addStatement("$T $N = new $T<>()", type_args, variable_args, HashMap.class);

        List<? extends VariableElement> params = method.getParameters();
        for (VariableElement param : params) {
            if (useEnumOrdinal(param)) {
                builder.addStatement("$N.put($S, $N.ordinal())", variable_args, param.getSimpleName(), param.getSimpleName());
                continue;
            }

            builder.addStatement("$N.put($S, $N)", variable_args, param.getSimpleName(), param.getSimpleName());
        }

        return builder.addStatement("sendMessage($N, $N)", methodId, variable_args)
                .build();
    }

    private boolean useEnumOrdinal(VariableElement param) {
        return isEnum(param) && isAnnotatedWithUseOrdinal(param);
    }

    private boolean isEnum(VariableElement param) {
        Element element = mTypes.asElement(param.asType());
        if (element != null) {
            return element.getKind() == ElementKind.ENUM;
        }

        return false;
    }

    private boolean isAnnotatedWithUseOrdinal(VariableElement param) {
        return param.getAnnotation(UseOrdinal.class) != null;
    }

    private TypeSpec buildDispatcher(TypeElement targetInterface, List<Pair<String, ExecutableElement>> methodIdPairs) {
        // WeakReference<targetInterface>
        ParameterizedTypeName WeakReference_targetInterface = ParameterizedTypeName.get(
                ClassName.get(WeakReference.class), ClassName.get(targetInterface));

        // Map<String, Object>
        ParameterizedTypeName type_data = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(Object.class)
        );

        // class: Dispatcher
        TypeSpec.Builder builder = TypeSpec.classBuilder("Dispatcher")
                .addSuperinterface(ClassName.get(Dispatcher.class))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

        // field
        final String field_callbackWeakReference = "callbackWeakReference";

        FieldSpec callbackWeakReference = FieldSpec.builder(WeakReference_targetInterface, field_callbackWeakReference)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();

        builder.addField(callbackWeakReference);

        // constructor
        final String param_callback = "callback";

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(targetInterface), param_callback)
                .addStatement("this.$N = new $T<>($N)", field_callbackWeakReference, WeakReference.class, param_callback)
                .build();

        builder.addMethod(constructor);

        // override: pubic boolean dispatch(Map<String, Object> data)
        final String method_dispatch = "dispatch";
        final String param_data = "data";
        final String variable_methodId = "methodId";
        final String variable_callback = "callback";

        MethodSpec.Builder dispatchBuilder = MethodSpec.methodBuilder(method_dispatch)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addAnnotation(ClassName.get(Override.class))
                .addParameter(type_data, param_data)
                .beginControlFlow("if (!$N.equals($N.get($N)))", FIELD_CLASS_NAME, param_data, FIELD_KEY_CLASS_NAME)
                .addStatement("return false")
                .endControlFlow()
                .addStatement("$T $N = ($T) $N.get($N)", TypeName.INT, variable_methodId, TypeName.INT, param_data, FIELD_KEY_METHOD_ID)
                .addStatement("$T $N = $N.get()", targetInterface, variable_callback, field_callbackWeakReference)
                .beginControlFlow("if ($N == null)", variable_callback)
                .addStatement("return false")
                .endControlFlow();

        dispatchBuilder.beginControlFlow("switch ($N)", variable_methodId);

        buildAllSwitchCase(dispatchBuilder, methodIdPairs, param_data, variable_callback);

        dispatchBuilder.endControlFlow()
                .addStatement("return false");

        return builder.addMethod(dispatchBuilder.build())
                .build();
    }

    private void buildAllSwitchCase(MethodSpec.Builder builder,
                                    List<Pair<String, ExecutableElement>> methodIdPairs,
                                    String param_data,
                                    String variable_callback) {
        for (Pair<String, ExecutableElement> methodPair : methodIdPairs) {
            buildSwitchCase(builder, methodPair, param_data, variable_callback);
        }
    }

    private void buildSwitchCase(MethodSpec.Builder builder,
                                 Pair<String, ExecutableElement> methodPair,
                                 String param_data,
                                 String variable_callback) {
        String methodId = methodPair.getKey();
        ExecutableElement method = methodPair.getValue();

        builder.addCode("case $N:\n", methodId);

        StringBuilder argsBuilder = new StringBuilder();
        for (VariableElement param : method.getParameters()) {
            String variable_name = methodId + "_" + param.getSimpleName();
            argsBuilder.append(variable_name)
                    .append(",");

            if (useEnumOrdinal(param)) {
                builder.addStatement("$T $N = $T.values()[(int) $N.get($S)]",
                        param.asType(), variable_name, param.asType(), param_data, param.getSimpleName());
                continue;
            }

            builder.addStatement("$T $N = ($T) $N.get($S)",
                    param.asType(), variable_name, param.asType(), param_data, param.getSimpleName());
        }

        String args = "";

        if (argsBuilder.length() > 0) {
            args = argsBuilder.substring(0, argsBuilder.length() - 1/*去掉参数列表中最后一个多余的逗号*/);
        }

        builder.addStatement("$N.$N(" + args + ")", variable_callback, method.getSimpleName())
                .addStatement("return true");
    }
}

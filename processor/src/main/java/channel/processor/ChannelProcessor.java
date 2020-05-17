package channel.processor;

import channel.helper.Channel;
import channel.helper.ParamInspector;
import com.squareup.javapoet.*;
import javafx.util.Pair;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({
        "channel.helper.Channel"
})
public class ChannelProcessor extends AbstractProcessor {
    private Types mTypes;
    private Messager mMessager;
    private Elements mElements;

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

        List<Pair<ExecutableElement, Integer>> methodIdPairs = generateAllMethodId(methods);

        TypeSpec channelWrapperType = buildChannelWrapper(channel.name(), targetInterface, methodIdPairs);
        TypeSpec emitterType = buildEmitter(methodIdPairs);
        TypeSpec dispatcherType = buildDispatcher(methodIdPairs);

        channelWrapperType = channelWrapperType.toBuilder()
//                .addType(emitterType)
//                .addType(dispatcherType)
                .build();

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

    private List<Pair<ExecutableElement, Integer>> generateAllMethodId(List<ExecutableElement> methods) {
        List<Pair<ExecutableElement, Integer>> methodIdPairs = new ArrayList<>(methods.size());

        for (int i = 1; i <= methods.size(); i++) {
            ExecutableElement method = methods.get(i - 1);
            methodIdPairs.add(new Pair<>(method, i));
        }

        return methodIdPairs;
    }

    private TypeSpec buildChannelWrapper(String name, TypeElement targetInterface, List<Pair<ExecutableElement, Integer>> methodIdPairs) {
        if ("".equals(name)) {
            name = targetInterface.getSimpleName() + "Channel";
        }

        ClassName string = ClassName.get("java.lang", "String");
        FieldSpec KEY_CLASS_NAME = FieldSpec.builder(string, "KEY_CLASS_NAME", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", "__class_name")
                .build();
        FieldSpec KEY_METHOD_ID = FieldSpec.builder(string, "KEY_METHOD_ID", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", "__method_id")
                .build();
        FieldSpec CLASS_NAME = FieldSpec.builder(string, "CLASS_NAME", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", targetInterface.getQualifiedName())
                .build();

        TypeSpec.Builder builder = TypeSpec.classBuilder(name)
                .addField(KEY_CLASS_NAME)
                .addField(KEY_METHOD_ID)
                .addField(CLASS_NAME)
                .addFields(generateMethodIdField(methodIdPairs));

        return builder.build();
    }

    private List<FieldSpec> generateMethodIdField(List<Pair<ExecutableElement, Integer>> methodIdPairs) {
        List<FieldSpec> methodIdFieldList = new ArrayList<>(methodIdPairs.size());

        for (Pair<ExecutableElement, Integer> methodIdPair : methodIdPairs) {
            methodIdFieldList.add(
                    FieldSpec.builder(TypeName.INT, "METHOD_ID_" + methodIdPair.getValue(), Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$L", methodIdPair.getValue())
                            .build()
            );
        }

        return methodIdFieldList;
    }

    private TypeSpec buildEmitter(List<Pair<ExecutableElement, Integer>> methodIdPairs) {
        // TODO
        return null;
    }

    private TypeSpec buildDispatcher(List<Pair<ExecutableElement, Integer>> methodIdPairs) {
        // TODO
        return null;
    }
}

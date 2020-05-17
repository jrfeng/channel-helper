package channel.helper;

import javax.lang.model.element.VariableElement;

public interface ParamInspector {
    boolean isIllegal(VariableElement param);
}

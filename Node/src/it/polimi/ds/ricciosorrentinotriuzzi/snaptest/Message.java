package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    private String methodName;
    private List<String> types;
    private List<Object> params;
    private Class<?>[] parameterTypes;
    private Object[] parameters;

    public Message(String methodName, Class<?>[] parameterTypes, Object[] parameters) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public Message(String methodName, List<String> types, List<Object> params) {
        this.methodName = methodName;
        this.types = types;
        this.params = params;
    }


    public String getMethodName() {
        return methodName;
    }

    public List<String> getTypes() {
        return types;
    }

    public List<Object> getParams() {
        return params;
    }

}

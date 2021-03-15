package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import java.io.Serializable;

public class Message implements Serializable {
    private String methodName;
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

    public String getMethodName() {
        return methodName;
    }

}

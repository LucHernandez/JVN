package jvn;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JvnProxy implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 1L;
    private JvnObject jvnObject;

    private JvnProxy(JvnObject jvnObject) {
        this.jvnObject = jvnObject;
    }
    public static Object newInstance(Object target, JvnObject jvnObject) {
        return Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new JvnProxy(jvnObject)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean isRead = method.isAnnotationPresent(JvnRead.class);
        boolean isWrite = method.isAnnotationPresent(JvnWrite.class);

        if (isWrite) {
            jvnObject.jvnLockWrite();
        } else if (isRead) {
            jvnObject.jvnLockRead();
        }

        try {
            Object state = jvnObject.jvnGetObjectState();
            return method.invoke(state, args);
        } finally {
            if (isRead || isWrite) {
                jvnObject.jvnUnLock();
            }
        }
    }
}
package jvn;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

public class JvnObjectImpl implements JvnObject{

    LockStatus lockStatus;
    Lock writeLock;
    Lock readLock;
    JvnServerImpl localServer;
    Serializable cachedObject;
    public final UUID uuid;

    public JvnObjectImpl(JvnServerImpl localServer, Serializable cachedObject, UUID id) {
        lockStatus = LockStatus.NL;
        this.localServer = localServer;
        this.cachedObject = cachedObject;
        uuid = id;
    }

    @Override
    public void jvnLockRead() throws JvnException {
        switch (lockStatus) {
            case RC -> lockStatus = LockStatus.R;
            case WC -> lockStatus = LockStatus.RWC;
            case R, RWC -> throw new RuntimeException("Read Lock already acquired");
            case NL -> {
                localServer.jvnLockRead(jvnGetObjectId());
                lockStatus = LockStatus.R;
            }
            case W -> throw new RuntimeException("Higher priority lock already acquired");
            case null, default ->  throw new JvnException("Invalid Lock Status");
        }
        readLock.lock();
    }

    @Override
    public void jvnLockWrite() throws JvnException {
        switch (lockStatus) {
            case WC, RWC -> lockStatus = LockStatus.W;
            case RC, R, NL -> {
                localServer.jvnLockWrite(jvnGetObjectId());
                lockStatus = LockStatus.W;
            }
            case W -> throw new RuntimeException("Write Lock already acquired");
            case null, default ->  throw new JvnException("Invalid Lock Status");
        }
        writeLock.lock();
    }

    @Override
    public void jvnUnLock() throws JvnException {
        switch (lockStatus) {
            case W -> lockStatus = LockStatus.WC;
            case R -> lockStatus = LockStatus.RC;
        }
        readLock.unlock();
        writeLock.unlock();
    }

    @Override
    public UUID jvnGetObjectId() throws JvnException {
        return uuid;
    }

    @Override
    public Serializable jvnGetObjectState() throws JvnException {
        return cachedObject;
    }

    private void barrierWait(Lock lock) {
        lock.lock();
        lock.unlock();
    }

    @Override
    public void jvnInvalidateReader() throws JvnException {
        if (lockStatus == LockStatus.R) {
            barrierWait(readLock);
        }
        lockStatus = LockStatus.NL;
    }

    @Override
    public Serializable jvnInvalidateWriter() throws JvnException {
        if (lockStatus == LockStatus.W) {
            barrierWait(writeLock);
        }
        lockStatus = LockStatus.NL;
        return cachedObject;
    }

    @Override
    public Serializable jvnInvalidateWriterForReader() throws JvnException {
        if (lockStatus == LockStatus.W) {
            barrierWait(writeLock);
        }
        readLock.lock();
        lockStatus = LockStatus.R;
        return cachedObject;
    }
}

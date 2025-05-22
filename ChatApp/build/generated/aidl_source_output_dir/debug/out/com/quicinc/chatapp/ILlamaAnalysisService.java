/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.quicinc.chatapp;
public interface ILlamaAnalysisService extends android.os.IInterface
{
  /** Default implementation for ILlamaAnalysisService. */
  public static class Default implements com.quicinc.chatapp.ILlamaAnalysisService
  {
    @Override public void analyzeText(java.lang.String text, com.quicinc.chatapp.IAnalysisCallback callback) throws android.os.RemoteException
    {
    }
    @Override public boolean isServiceReady() throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean isServiceInitializing() throws android.os.RemoteException
    {
      return false;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.quicinc.chatapp.ILlamaAnalysisService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.quicinc.chatapp.ILlamaAnalysisService interface,
     * generating a proxy if needed.
     */
    public static com.quicinc.chatapp.ILlamaAnalysisService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.quicinc.chatapp.ILlamaAnalysisService))) {
        return ((com.quicinc.chatapp.ILlamaAnalysisService)iin);
      }
      return new com.quicinc.chatapp.ILlamaAnalysisService.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_analyzeText:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          com.quicinc.chatapp.IAnalysisCallback _arg1;
          _arg1 = com.quicinc.chatapp.IAnalysisCallback.Stub.asInterface(data.readStrongBinder());
          this.analyzeText(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_isServiceReady:
        {
          boolean _result = this.isServiceReady();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_isServiceInitializing:
        {
          boolean _result = this.isServiceInitializing();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.quicinc.chatapp.ILlamaAnalysisService
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void analyzeText(java.lang.String text, com.quicinc.chatapp.IAnalysisCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(text);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_analyzeText, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean isServiceReady() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isServiceReady, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean isServiceInitializing() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isServiceInitializing, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_analyzeText = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_isServiceReady = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_isServiceInitializing = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
  }
  public static final java.lang.String DESCRIPTOR = "com.quicinc.chatapp.ILlamaAnalysisService";
  public void analyzeText(java.lang.String text, com.quicinc.chatapp.IAnalysisCallback callback) throws android.os.RemoteException;
  public boolean isServiceReady() throws android.os.RemoteException;
  public boolean isServiceInitializing() throws android.os.RemoteException;
}

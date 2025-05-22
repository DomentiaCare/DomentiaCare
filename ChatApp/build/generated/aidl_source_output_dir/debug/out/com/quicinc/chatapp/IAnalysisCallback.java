/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.quicinc.chatapp;
public interface IAnalysisCallback extends android.os.IInterface
{
  /** Default implementation for IAnalysisCallback. */
  public static class Default implements com.quicinc.chatapp.IAnalysisCallback
  {
    @Override public void onResult(java.lang.String result) throws android.os.RemoteException
    {
    }
    @Override public void onError(java.lang.String error) throws android.os.RemoteException
    {
    }
    @Override public void onNoResult() throws android.os.RemoteException
    {
    }
    @Override public void onPartialResult(java.lang.String partialText) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.quicinc.chatapp.IAnalysisCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.quicinc.chatapp.IAnalysisCallback interface,
     * generating a proxy if needed.
     */
    public static com.quicinc.chatapp.IAnalysisCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.quicinc.chatapp.IAnalysisCallback))) {
        return ((com.quicinc.chatapp.IAnalysisCallback)iin);
      }
      return new com.quicinc.chatapp.IAnalysisCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onResult:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.onResult(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onError:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.onError(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onNoResult:
        {
          this.onNoResult();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onPartialResult:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.onPartialResult(_arg0);
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.quicinc.chatapp.IAnalysisCallback
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
      @Override public void onResult(java.lang.String result) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(result);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onResult, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onError(java.lang.String error) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(error);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onError, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onNoResult() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onNoResult, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onPartialResult(java.lang.String partialText) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(partialText);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPartialResult, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onNoResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onPartialResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
  }
  public static final java.lang.String DESCRIPTOR = "com.quicinc.chatapp.IAnalysisCallback";
  public void onResult(java.lang.String result) throws android.os.RemoteException;
  public void onError(java.lang.String error) throws android.os.RemoteException;
  public void onNoResult() throws android.os.RemoteException;
  public void onPartialResult(java.lang.String partialText) throws android.os.RemoteException;
}

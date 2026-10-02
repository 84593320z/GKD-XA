/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: C:\\Users\\Y-H\\AppData\\Local\\Android\\Sdk\\build-tools\\37.0.0\\aidl.exe -pC:\\Users\\Y-H\\AppData\\Local\\Android\\Sdk\\platforms\\android-37.0\\framework.aidl -oE:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\build\\generated\\aidl_source_output_dir\\gkdDebug\\out -IE:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\src\\main\\aidl -IE:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\src\\gkd\\aidl -IE:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\src\\debug\\aidl -IE:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\src\\gkdDebug\\aidl -IC:\\Users\\Y-H\\.gradle\\caches\\9.7.1\\transforms\\a02c48fb66d302f1e31f22e84d3acd3c\\transformed\\core-1.19.0\\aidl -IC:\\Users\\Y-H\\.gradle\\caches\\9.7.1\\transforms\\8402fb9470982f6cb1d08b61caafb233\\transformed\\priv-core-0.16.1\\aidl -IC:\\Users\\Y-H\\.gradle\\caches\\9.7.1\\transforms\\8469a3e6f7b46ed5f7fd05111d31edfe\\transformed\\versionedparcelable-1.1.1\\aidl -dC:\\Users\\Y-H\\AppData\\Local\\Temp\\aidl9805876332161359537.d E:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\src\\main\\aidl\\li\\gkd\\app\\priv\\IUserService.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package li.gkd.app.priv;
public interface IUserService extends android.os.IInterface
{
  /** Default implementation for IUserService. */
  public static class Default implements li.gkd.app.priv.IUserService
  {
    @Override public void destroy() throws android.os.RemoteException
    {
    }
    @Override public android.graphics.Bitmap takeScreenshot(android.graphics.Rect crop, int rotation) throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements li.gkd.app.priv.IUserService
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an li.gkd.app.priv.IUserService interface,
     * generating a proxy if needed.
     */
    public static li.gkd.app.priv.IUserService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof li.gkd.app.priv.IUserService))) {
        return ((li.gkd.app.priv.IUserService)iin);
      }
      return new li.gkd.app.priv.IUserService.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(DESCRIPTOR);
      }
      switch (code)
      {
        case TRANSACTION_destroy:
        {
          this.destroy();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_takeScreenshot:
        {
          android.graphics.Rect _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.graphics.Rect.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          android.graphics.Bitmap _result = this.takeScreenshot(_arg0, _arg1);
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static final class Proxy implements li.gkd.app.priv.IUserService
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
      public final java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void destroy() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_destroy, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public android.graphics.Bitmap takeScreenshot(android.graphics.Rect crop, int rotation) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.graphics.Bitmap _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, crop, 0);
          _data.writeInt(rotation);
          boolean _status = mRemote.transact(Stub.TRANSACTION_takeScreenshot, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, android.graphics.Bitmap.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_destroy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777114);
    static final int TRANSACTION_takeScreenshot = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "li.gkd.app.priv.IUserService";
  public void destroy() throws android.os.RemoteException;
  public android.graphics.Bitmap takeScreenshot(android.graphics.Rect crop, int rotation) throws android.os.RemoteException;
  /** @hide */
  static class _Parcel {
    static private <T> T readTypedObject(
        android.os.Parcel parcel,
        android.os.Parcelable.Creator<T> c) {
      if (parcel.readInt() != 0) {
          return c.createFromParcel(parcel);
      } else {
          return null;
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedObject(
        android.os.Parcel parcel, T value, int parcelableFlags) {
      if (value != null) {
        parcel.writeInt(1);
        value.writeToParcel(parcel, parcelableFlags);
      } else {
        parcel.writeInt(0);
      }
    }
  }
}

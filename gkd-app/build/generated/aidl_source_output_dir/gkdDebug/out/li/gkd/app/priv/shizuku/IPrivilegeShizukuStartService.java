/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: C:\\Users\\Y-H\\AppData\\Local\\Android\\Sdk\\build-tools\\37.0.0\\aidl.exe -pC:\\Users\\Y-H\\AppData\\Local\\Android\\Sdk\\platforms\\android-37.0\\framework.aidl -oE:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\build\\generated\\aidl_source_output_dir\\gkdDebug\\out -IE:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\src\\main\\aidl -IE:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\src\\gkd\\aidl -IE:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\src\\debug\\aidl -IE:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\src\\gkdDebug\\aidl -IC:\\Users\\Y-H\\.gradle\\caches\\9.7.1\\transforms\\c67f836bcc46785dd4119267a129e1e5\\transformed\\core-1.19.0\\aidl -IC:\\Users\\Y-H\\.gradle\\caches\\9.7.1\\transforms\\55814b1a31e9e11ef5be52c28e9cf068\\transformed\\priv-core-0.16.1\\aidl -IC:\\Users\\Y-H\\.gradle\\caches\\9.7.1\\transforms\\a48d0ac01f98831aea4c07048f7d2249\\transformed\\versionedparcelable-1.1.1\\aidl -dC:\\Users\\Y-H\\AppData\\Local\\Temp\\aidl8736067025078633095.d E:\\yuanbao\\2026-10-02-16-39-45\\mine\\gkd-app\\src\\main\\aidl\\li\\gkd\\app\\priv\\shizuku\\IPrivilegeShizukuStartService.aidl
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package li.gkd.app.priv.shizuku;
public interface IPrivilegeShizukuStartService extends android.os.IInterface
{
  /** Default implementation for IPrivilegeShizukuStartService. */
  public static class Default implements li.gkd.app.priv.shizuku.IPrivilegeShizukuStartService
  {
    @Override public void start(java.lang.String commandLine, android.os.ParcelFileDescriptor stdout, android.os.ParcelFileDescriptor stderr, android.os.ResultReceiver resultReceiver) throws android.os.RemoteException
    {
    }
    @Override public void destroy() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements li.gkd.app.priv.shizuku.IPrivilegeShizukuStartService
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an li.gkd.app.priv.shizuku.IPrivilegeShizukuStartService interface,
     * generating a proxy if needed.
     */
    public static li.gkd.app.priv.shizuku.IPrivilegeShizukuStartService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof li.gkd.app.priv.shizuku.IPrivilegeShizukuStartService))) {
        return ((li.gkd.app.priv.shizuku.IPrivilegeShizukuStartService)iin);
      }
      return new li.gkd.app.priv.shizuku.IPrivilegeShizukuStartService.Stub.Proxy(obj);
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
        case TRANSACTION_start:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          android.os.ParcelFileDescriptor _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.os.ParcelFileDescriptor.CREATOR);
          android.os.ParcelFileDescriptor _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.os.ParcelFileDescriptor.CREATOR);
          android.os.ResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, android.os.ResultReceiver.CREATOR);
          this.start(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_destroy:
        {
          this.destroy();
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
    private static final class Proxy implements li.gkd.app.priv.shizuku.IPrivilegeShizukuStartService
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
      @Override public void start(java.lang.String commandLine, android.os.ParcelFileDescriptor stdout, android.os.ParcelFileDescriptor stderr, android.os.ResultReceiver resultReceiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(commandLine);
          _Parcel.writeTypedObject(_data, stdout, 0);
          _Parcel.writeTypedObject(_data, stderr, 0);
          _Parcel.writeTypedObject(_data, resultReceiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_start, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
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
    }
    static final int TRANSACTION_start = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_destroy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777114);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "li.gkd.app.priv.shizuku.IPrivilegeShizukuStartService";
  public void start(java.lang.String commandLine, android.os.ParcelFileDescriptor stdout, android.os.ParcelFileDescriptor stderr, android.os.ResultReceiver resultReceiver) throws android.os.RemoteException;
  public void destroy() throws android.os.RemoteException;
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

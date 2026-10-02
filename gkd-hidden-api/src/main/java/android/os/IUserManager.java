package android.os;

public interface IUserManager extends IInterface {
    abstract class Stub extends Binder implements IUserManager {
        public static IUserManager asInterface(IBinder obj) {
            throw new RuntimeException();
        }
    }
}

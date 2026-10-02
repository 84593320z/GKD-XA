package android.os;

import android.content.Context;
import android.content.pm.UserInfo;

import java.util.List;

import li.songe.remap.RemapType;

@RemapType(UserManager.class)
public class UserManagerHidden {

    public UserManagerHidden(Context context, IUserManager service) {
        throw new RuntimeException();
    }

    public List<UserInfo> getUsers(boolean excludeDying) {
        throw new RuntimeException();
    }
}

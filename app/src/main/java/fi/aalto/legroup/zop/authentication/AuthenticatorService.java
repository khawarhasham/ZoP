package fi.aalto.legroup.zop.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * The service that lets Android know about the custom Authenticator.
 */
public final class AuthenticatorService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        Authenticator authenticator = new Authenticator(this);
        return authenticator.getIBinder();
    }

}

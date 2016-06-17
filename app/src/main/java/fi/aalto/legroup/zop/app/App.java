package fi.aalto.legroup.zop.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.rollbar.android.Rollbar;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.otto.Bus;

import java.io.File;
import java.security.GeneralSecurityException;

import fi.aalto.legroup.zop.BuildConfig;
import fi.aalto.legroup.zop.R;
import fi.aalto.legroup.zop.authentication.AuthenticatedHttpClient;
import fi.aalto.legroup.zop.authentication.LoginManager;
import fi.aalto.legroup.zop.authentication.LoginRequestEvent;
import fi.aalto.legroup.zop.authentication.OIDCConfig;
import fi.aalto.legroup.zop.authoring.LocationManager;
import fi.aalto.legroup.zop.entities.serialization.json.JsonSerializer;
import fi.aalto.legroup.zop.storage.CombinedVideoRepository;
import fi.aalto.legroup.zop.storage.VideoInfoRepository;
import fi.aalto.legroup.zop.storage.VideoRepository;
import fi.aalto.legroup.zop.storage.remote.SyncService;
import fi.aalto.legroup.zop.storage.remote.UploadService;
import fi.aalto.legroup.zop.storage.remote.strategies.AchRailsStrategy;
import fi.aalto.legroup.zop.storage.remote.strategies.ClViTra2Strategy;
import fi.aalto.legroup.zop.storage.remote.strategies.GoViTraStrategy;
import fi.legroup.aalto.cryptohelper.CryptoHelper;

public final class App extends MultiDexApplication
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ACH_SO_LOCAL_STORAGE_NAME = "ZoP"; //KH: change

    public static Bus bus;

    public static ConnectivityManager connectivityManager;

    public static LoginManager loginManager;
    public static OkHttpClient httpClient;
    public static AuthenticatedHttpClient authenticatedHttpClient;
    public static LocationManager locationManager;

    public static JsonSerializer jsonSerializer;

    private static CombinedVideoRepository combinedRepository;
    public static VideoRepository videoRepository;
    public static VideoInfoRepository videoInfoRepository;

    public static File localStorageDirectory;
    public static File cacheVideoDirectoryBase;

    public static AchRailsStrategy achRails;

    private static Uri layersBoxUrl;
    private static Uri publicLayersBoxUrl;
    private static boolean usePublicLayersBox;

    @Override
    public void onCreate() {
        super.onCreate();

        setupErrorReporting();

        setupPreferences();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        layersBoxUrl = readLayersBoxUrl();
        usePublicLayersBox = preferences.getBoolean(AppPreferences.USE_PUBLIC_LAYERS_BOX, false);
        System.out.println("Setting publicLayersBox to false");
        usePublicLayersBox = false;
        System.out.println("Khawar ZoP: usePublicLayersBox " + usePublicLayersBox );
        publicLayersBoxUrl = Uri.parse(getString(R.string.publicLayersBoxUrl));

        bus = new AppBus();

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        httpClient = new OkHttpClient();
        authenticatedHttpClient = new AuthenticatedHttpClient(this, httpClient);

        loginManager = new LoginManager(this, bus);

        locationManager = new LocationManager(this);

        jsonSerializer = new JsonSerializer();

        // TODO: The instantiation of repositories should be abstracted further.
        // That would allow for multiple repositories.
        File mediaDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        localStorageDirectory = new File(mediaDirectory, ACH_SO_LOCAL_STORAGE_NAME);

        if (!(localStorageDirectory.isDirectory() || localStorageDirectory.mkdirs())) {
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
        }

        cacheVideoDirectoryBase = new File(localStorageDirectory, "cache");
        cacheVideoDirectoryBase.mkdirs();

        if (!cacheVideoDirectoryBase.isDirectory()) {
            // If the cache directory can't be created use internal App storage
            // There is a slim chance that the user saves some modified videos to the internal
            // storage and then inserts and SD card causing videos not to be uploaded, but it
            // doesn't seem worth the trouble.

            File internalDataDirectory = new File(getApplicationInfo().dataDir);
            cacheVideoDirectoryBase = new File(internalDataDirectory, "manifestcache");
            cacheVideoDirectoryBase.mkdirs();
        }

        combinedRepository = new CombinedVideoRepository(bus, jsonSerializer,
                localStorageDirectory, makeCacheVideoDirectory());

        videoRepository = combinedRepository;
        videoInfoRepository = combinedRepository;

        setupUploaders(this);

        videoRepository.refreshOffline();

        // Run migrations to update old videos.
        videoRepository.migrateVideos(this);

        updateOIDCTokens(this);

        bus.post(new LoginRequestEvent(LoginRequestEvent.Type.LOGIN));

        // Trim the caches asynchronously
        AppCache.trim(this);

        // Setup Google Analytics
        AppAnalytics.setup(this);
    }

    public static File makeCacheVideoDirectory() {
        String host = getLayersBoxUrl().getHost().replace('.', '_');
        File path = new File(cacheVideoDirectoryBase, host);
        path.mkdirs();

        return path;
    }

    public static void updateOIDCTokens(final Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (preferences.getBoolean(AppPreferences.USE_PUBLIC_LAYERS_BOX, false)) {
            OIDCConfig.setTokens(context.getString(R.string.oidcClientId), context.getString(R.string.oidcClientSecret));
            SyncService.syncWithCloudStorage(context);
        } else {
            OIDCConfig.retrieveOIDCTokens(context, new OIDCConfig.TokenCallback() {
                @Override
                public void tokensRetrieved() {
                    SyncService.syncWithCloudStorage(context);
                }
            });
        }
    }

    public static Uri getLayersBoxUrl() {
        if (usePublicLayersBox) {
            return publicLayersBoxUrl;
        } else {
            return layersBoxUrl;
        }
    }

    public static Uri getLayersServiceUrl(String serviceUriString) {
        System.out.println("Khawar ZoP:" + serviceUriString);
        return getLayersServiceUrl(Uri.parse(serviceUriString));
    }

    public static Uri getLayersServiceUrl(Uri serviceUri) {
        return resolveRelativeUri(serviceUri, getLayersBoxUrl());
    }

    public static Uri getAchRailsUrl(Context context) {
        if (usePublicLayersBox) {
            return getLayersServiceUrl(context.getString(R.string.publicAchRailsUrl));
        } else {
            return getLayersServiceUrl(context.getString(R.string.achRailsUrl));
        }
    }

    public static Uri getAchsoStorageUrl(Context context) {
        if (usePublicLayersBox) {
            return getLayersServiceUrl(context.getString(R.string.publicAchsoStorageUrl));
        } else {
            return getLayersServiceUrl(context.getString(R.string.achsoStorageUrl));
        }
    }

    /**
     * If the given URI is relative, resolves it into an absolute one against the given root. If
     * the URI is already absolute, it will be returned as-is.
     *
     * @param uri     Relative or absolute URI.
     * @param rootUri Absolute URI to use as the root in case the given URI is relative.
     * @return An absolute URI.
     */
    private static Uri resolveRelativeUri(Uri uri, Uri rootUri) {
        System.out.println("Khawar ZoP: " + uri.toString());
        System.out.println("Khawar ZoP: rootUri " + rootUri.toString());
        if (uri.isAbsolute()) {
            return uri;
        } else {
            // Remove a leading slash if there is one, otherwise it'll be duplicated
            String path = uri.toString().replaceFirst("^/", "");
            System.out.println("Khawar ZoP: " + uri.toString() + " path " + path);

            return rootUri.buildUpon().appendEncodedPath(path).build();
        }
    }

    public static boolean isConnected() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean isDisconnected() {
        return !isConnected();
    }

    private void setupPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void setupErrorReporting() {
        String releaseStage;

        if (BuildConfig.DEBUG) {
            releaseStage = "debug";
        } else {
            releaseStage = "production";
        }

        Rollbar.init(this, getString(R.string.rollbarApiKey), releaseStage);
    }

    public static void setupUploaders(Context context) {

        combinedRepository.clear();
        UploadService.clearUploaders();

        achRails = new AchRailsStrategy(jsonSerializer, getAchRailsUrl(context));
        combinedRepository.addHost(achRails);
        combinedRepository.setCacheRoot(makeCacheVideoDirectory());

        UploadService.addUploader(new GoViTraStrategy(jsonSerializer, getAchsoStorageUrl(context)));

        Uri clViTra2Url = Uri.parse(context.getString(R.string.clvitra2Url));
        ClViTra2Strategy videoStrategy = new ClViTra2Strategy(clViTra2Url);

        UploadService.addUploader(videoStrategy);
    }

    private Uri readLayersBoxUrl() {

        String[] searchPackages = {
                "com.raycom.ltb",
                "fi.legroup.aalto.achsoexampleconnection",
        };

        for (String packageName : searchPackages) {
            try {
                Context context = createPackageContext(packageName, 0);
                SharedPreferences ltbPreferences = context.getSharedPreferences(
                        "eu.learning-layers.LAYERS_BOX", Context.MODE_PRIVATE);

                String encryptedUrlString = ltbPreferences.getString("LAYERS_BOX_URL", null);
                if (encryptedUrlString != null) {
                    String secret = getString(R.string.ltbSecret);
                    String urlString = CryptoHelper.decrypt(encryptedUrlString, secret);
                    return Uri.parse(urlString);
                }
            } catch (PackageManager.NameNotFoundException | GeneralSecurityException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String defaultUrlString = getString(R.string.layersBoxUrl);
        String urlString = preferences.getString(AppPreferences.LAYERS_BOX_URL, defaultUrlString);

        return Uri.parse(urlString);
    }

    /**
     * Listens for changes to the shared preferences.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        switch (key) {
            // Listen for changes to the Layers box URL preference and update the internal field.
            case AppPreferences.LAYERS_BOX_URL:
                String defaultUrlString = getString(R.string.layersBoxUrl);
                String urlString = preferences.getString(key, defaultUrlString);

                layersBoxUrl = Uri.parse(urlString);
                break;

            // Listen for changes to the analytics opt in preference.
            case AppPreferences.ANALYTICS_OPT_IN:
                boolean hasOptedIn = preferences.getBoolean(key, false);
                GoogleAnalytics.getInstance(this).setAppOptOut(!hasOptedIn);
                break;

            case AppPreferences.USE_PUBLIC_LAYERS_BOX:
                usePublicLayersBox = preferences.getBoolean(key, false);
                break;
        }
    }

}

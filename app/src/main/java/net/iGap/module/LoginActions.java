package net.iGap.module;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import io.realm.Realm;
import net.iGap.G;
import net.iGap.activities.ActivityMain;
import net.iGap.helper.HelperClientCondition;
import net.iGap.interfaces.OnSecuring;
import net.iGap.interfaces.OnUserInfoResponse;
import net.iGap.interfaces.OnUserLogin;
import net.iGap.proto.ProtoGlobal;
import net.iGap.realm.RealmPhoneContacts;
import net.iGap.realm.RealmRegisteredInfo;
import net.iGap.realm.RealmUserInfo;
import net.iGap.request.RequestClientGetRoomList;
import net.iGap.request.RequestQueue;
import net.iGap.request.RequestUserContactsGetBlockedList;
import net.iGap.request.RequestUserInfo;
import net.iGap.request.RequestUserLogin;
import net.iGap.request.RequestWrapper;

import static net.iGap.G.authorHash;
import static net.iGap.G.context;
import static net.iGap.G.firstEnter;
import static net.iGap.G.firstTimeEnterToApp;
import static net.iGap.G.isAppInFg;
import static net.iGap.G.isSendContact;
import static net.iGap.G.userId;

/**
 * all actions that need doing after login
 */
public class LoginActions extends Application {

    public LoginActions() {
        initSecureInterface();
    }

    /**
     * initialize securing interface for detecting
     * securing is done and continue login actions
     */
    private void initSecureInterface() {
        G.onSecuring = new OnSecuring() {
            @Override
            public void onSecure() {
                login();
            }
        };
    }

    /**
     * try login to server and do common actions
     */
    public static void login() {

        G.onUserLogin = new OnUserLogin() {
            @Override
            public void onLogin() {

                G.handler.post(new Runnable() {
                    @Override public void run() {
                        G.clientConditionGlobal = HelperClientCondition.computeClientCondition(null);
                        /**
                         * in first enter to app client send clientCondition after get room list
                         * but, in another login when user not closed app after login client send
                         * latest state to server without get room list , also if
                         * app in background is running now if firstTimeEnterToApp is false we send
                         * client condition because this field(firstTimeEnterToApp) will be lost value
                         * in close app and after app start running in background we don't send
                         * client condition!!! for avoid from this problem we checked isAppInFg state
                         * app is background send clientCondition (: .
                         */
                        if (!firstTimeEnterToApp || !isAppInFg) {
                            getRoomList();
                        }

                        if (firstEnter) {
                            firstEnter = false;
                            new RequestUserContactsGetBlockedList().userContactsGetBlockedList();
                            importContact();
                        }
                        getUserInfo();
                        //sendWaitingRequestWrappers();
                    }
                });



            }

            @Override
            public void onLoginError(int majorCode, int minorCode) {

            }
        };

        G.handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (G.isSecure) {
                    Realm realm = Realm.getDefaultInstance();
                    RealmUserInfo userInfo = realm.where(RealmUserInfo.class).findFirst();

                    if (userInfo != null) {
                        userId = userInfo.getUserId();
                        authorHash = userInfo.getAuthorHash();
                    }

                    if (!G.userLogin && userInfo != null && userInfo.getUserRegistrationState()) {
                        new RequestUserLogin().userLogin(userInfo.getToken());
                    }
                    realm.close();
                } else {
                    login();
                }
            }
        }, 500);
    }

    private static void getRoomList() {

        int _limit = ActivityMain.curentMainRoomListPosition + 20;

        if (_limit < 100) {
            new RequestClientGetRoomList().clientGetRoomList(0, _limit, true);
        } else {

            new RequestClientGetRoomList().clientGetRoomList(0, 100, true);

            int offset = 100;

            for (int i = 100; i < _limit; i += 100) {
                offset += 100;

                new RequestClientGetRoomList().clientGetRoomList(i, 100);
            }

            if (offset < _limit) {
                new RequestClientGetRoomList().clientGetRoomList(offset, 100);
            }
        }
    }

    private static void getUserInfo() {
        Realm realm = Realm.getDefaultInstance();
        final long userId = realm.where(RealmUserInfo.class).findFirst().getUserId();
        realm.close();

        G.onUserInfoResponse = new OnUserInfoResponse() {
            @Override
            public void onUserInfo(final ProtoGlobal.RegisteredUser user, String identity) {
                // fill own user info
                if (userId == user.getId()) {
                    Realm realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            RealmRegisteredInfo.putOrUpdate(user);
                        }
                    });

                    realm.close();
                }
            }

            @Override
            public void onUserInfoTimeOut() {

            }

            @Override
            public void onUserInfoError(int majorCode, int minorCode) {

            }
        };
        new RequestUserInfo().userInfo(userId);
    }

    public static void importContact() {
        /**
         * just import contact in each enter to app
         * when user login was done
         */
        if (isSendContact) {
            return;
        }

        if (G.userLogin) {
            /**
             * this can be go in the activity for check permission in api 6+
             */
            isSendContact = true;
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                G.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        RealmPhoneContacts.sendContactList(Contacts.getListOfContact(), false);
                    }
                });
            }
        } else {
            G.handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    importContact();
                }
            }, 2000);
        }
    }

    /**
     * resend some of requests
     */
    public static void sendWaitingRequestWrappers() {
        for (RequestWrapper requestWrapper : RequestQueue.WAITING_REQUEST_WRAPPERS) {
            RequestQueue.RUNNING_REQUEST_WRAPPERS.add(requestWrapper);
        }
        RequestQueue.WAITING_REQUEST_WRAPPERS.clear();

        for (int i = 0; i < RequestQueue.RUNNING_REQUEST_WRAPPERS.size(); i++) {
            final int j = i;
            G.handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        RequestQueue.sendRequest(RequestQueue.RUNNING_REQUEST_WRAPPERS.get(j));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    if (j == (RequestQueue.RUNNING_REQUEST_WRAPPERS.size() - 1)) {
                        RequestQueue.RUNNING_REQUEST_WRAPPERS.clear();
                    }
                }
            }, 1000 * j);
        }
    }
}
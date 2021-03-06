/*
* This is the source code of iGap for Android
* It is licensed under GNU AGPL v3.0
* You should have received a copy of the license in this archive (see LICENSE).
* Copyright © 2017 , iGap - www.iGap.net
* iGap Messenger | Free, Fast and Secure instant messaging application
* The idea of the RooyeKhat Media Company - www.RooyeKhat.co
* All rights reserved.
*/

package net.iGap.response;

import android.os.Handler;
import android.os.Looper;
import io.realm.Realm;
import net.iGap.Config;
import net.iGap.helper.HelperTimeOut;
import net.iGap.proto.ProtoGlobal;
import net.iGap.proto.ProtoUserContactsGetList;
import net.iGap.realm.RealmAvatar;
import net.iGap.realm.RealmContacts;
import net.iGap.realm.RealmRegisteredInfo;
import net.iGap.realm.RealmRegisteredInfoFields;

public class UserContactsGetListResponse extends MessageHandler {

    public int actionId;
    public Object message;
    public String identity;

    public UserContactsGetListResponse(int actionId, Object protoClass, String identity) {
        super(actionId, protoClass, identity);

        this.message = protoClass;
        this.actionId = actionId;
        this.identity = identity;
    }

    private static long getListTime;

    @Override public void handler() {
        super.handler();
        final ProtoUserContactsGetList.UserContactsGetListResponse.Builder builder = (ProtoUserContactsGetList.UserContactsGetListResponse.Builder) message;

        /**
         * for avoid from multiple running in same time.
         * (( hint : we have an error for this class and now use from timeout.
         * in next version of app will be checked that any of users get this error again or no ))
         */
        if (HelperTimeOut.timeoutChecking(0, getListTime, Config.GET_CONTACT_LIST_TIME_OUT)) {
            getListTime = System.currentTimeMillis();

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override public void run() {
                    final Realm realm = Realm.getDefaultInstance();

                    realm.executeTransactionAsync(new Realm.Transaction() {
                        @Override public void execute(Realm realm) {

                            realm.delete(RealmContacts.class);

                            for (ProtoGlobal.RegisteredUser registerUser : builder.getRegisteredUserList()) {
                                RealmRegisteredInfo realmRegisteredInfo = realm.where(RealmRegisteredInfo.class).equalTo(RealmRegisteredInfoFields.ID, registerUser.getId()).findFirst();
                                if (realmRegisteredInfo == null) {
                                    realmRegisteredInfo = realm.createObject(RealmRegisteredInfo.class, registerUser.getId());
                                    realmRegisteredInfo.setDoNotshowSpamBar(false);
                                }
                                realmRegisteredInfo.fillRegisteredUserInfo(registerUser, realmRegisteredInfo);

                                RealmContacts listResponse = realm.createObject(RealmContacts.class);
                                listResponse.setId(registerUser.getId());
                                listResponse.setUsername(registerUser.getUsername());
                                listResponse.setPhone(registerUser.getPhone());
                                listResponse.setFirst_name(registerUser.getFirstName());
                                listResponse.setLast_name(registerUser.getLastName());
                                listResponse.setDisplay_name(registerUser.getDisplayName());
                                listResponse.setInitials(registerUser.getInitials());
                                listResponse.setColor(registerUser.getColor());
                                listResponse.setStatus(registerUser.getStatus().toString());
                                listResponse.setLast_seen(registerUser.getLastSeen());
                                listResponse.setAvatarCount(registerUser.getAvatarCount());
                                listResponse.setCacheId(registerUser.getCacheId());
                                listResponse.setAvatar(RealmAvatar.put(registerUser.getId(), registerUser.getAvatar(), true));
                            }
                        }
                    }, new Realm.Transaction.OnSuccess() {
                        @Override public void onSuccess() {

                            realm.close();
                        }
                    }, new Realm.Transaction.OnError() {
                        @Override public void onError(Throwable error) {
                            realm.close();
                        }
                    });
                }
            });
        }
    }

    @Override public void timeOut() {
        super.timeOut();
    }

    @Override public void error() {
        super.error();
    }
}



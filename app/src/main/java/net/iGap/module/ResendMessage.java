/*
* This is the source code of iGap for Android
* It is licensed under GNU AGPL v3.0
* You should have received a copy of the license in this archive (see LICENSE).
* Copyright © 2017 , iGap - www.iGap.net
* iGap Messenger | Free, Fast and Secure instant messaging application
* The idea of the RooyeKhat Media Company - www.RooyeKhat.co
* All rights reserved.
*/

package net.iGap.module;

import android.content.Context;
import io.realm.Realm;
import java.util.List;
import net.iGap.G;
import net.iGap.helper.HelperUploadFile;
import net.iGap.interfaces.IResendMessage;
import net.iGap.module.structs.StructMessageInfo;
import net.iGap.proto.ProtoGlobal;
import net.iGap.realm.RealmRoom;
import net.iGap.realm.RealmRoomFields;
import net.iGap.realm.RealmRoomMessage;
import net.iGap.realm.RealmRoomMessageFields;

public class ResendMessage implements IResendMessage {
    private List<StructMessageInfo> mMessages;
    private IResendMessage mListener;
    private long mSelectedMessageID;

    public ResendMessage(Context context, IResendMessage listener, long selectedMessageID, List<StructMessageInfo> messages) {
        this.mMessages = messages;
        this.mListener = listener;
        this.mSelectedMessageID = selectedMessageID;
        AppUtils.buildResendDialog(context, messages.size(), this).show();
    }

    public List<StructMessageInfo> getMessages() {
        return mMessages;
    }

    @Override public void deleteMessage() {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override public void execute(Realm realm) {
                for (StructMessageInfo message : mMessages) {
                    RealmRoomMessage roomMessage = realm.where(RealmRoomMessage.class).equalTo(RealmRoomMessageFields.MESSAGE_ID, Long.parseLong(message.messageID)).findFirst();
                    if (roomMessage != null) {
                        roomMessage.deleteFromRealm();
                    }
                }
            }
        });
        realm.close();

        mListener.deleteMessage();
    }

    private void resend(final boolean all) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override public void execute(Realm realm) {
                for (StructMessageInfo message : mMessages) {
                    if (all) {
                        RealmRoomMessage roomMessage = realm.where(RealmRoomMessage.class).equalTo(RealmRoomMessageFields.MESSAGE_ID, Long.parseLong(message.messageID)).findFirst();
                        if (roomMessage != null) {
                            roomMessage.setStatus(ProtoGlobal.RoomMessageStatus.SENDING.toString());
                        }
                    } else {
                        if (message.messageID.equalsIgnoreCase(Long.toString(mSelectedMessageID))) {
                            RealmRoomMessage roomMessage = realm.where(RealmRoomMessage.class).equalTo(RealmRoomMessageFields.MESSAGE_ID, Long.parseLong(message.messageID)).findFirst();
                            if (roomMessage != null) {
                                roomMessage.setStatus(ProtoGlobal.RoomMessageStatus.SENDING.toString());
                            }
                            break;
                        }
                    }
                }
            }
        });

        if (all) {
            mListener.resendAllMessages();
        } else {
            mListener.resendMessage();
        }

        for (int i = 0; i < mMessages.size(); i++) {
            final int j = i;
            if (all) {
                G.handler.postDelayed(new Runnable() {
                    @Override public void run() {
                        Realm realm = Realm.getDefaultInstance();
                        RealmRoomMessage roomMessage = realm.where(RealmRoomMessage.class).equalTo(RealmRoomMessageFields.MESSAGE_ID, Long.parseLong(mMessages.get(j).messageID)).findFirst();
                        if (roomMessage != null) {
                            RealmRoom realmRoom = realm.where(RealmRoom.class).equalTo(RealmRoomFields.ID, roomMessage.getRoomId()).findFirst();
                            if (realmRoom != null) {
                                if (roomMessage.getAttachment() == null) {
                                    ProtoGlobal.Room.Type roomType = realmRoom.getType();
                                    G.chatSendMessageUtil.build(roomType, roomMessage.getRoomId(), roomMessage);
                                } else {
                                    HelperUploadFile.startUploadTaskChat(roomMessage.getRoomId(), realmRoom.getType(), roomMessage.getAttachment().getLocalFilePath(), roomMessage.getMessageId(),
                                        roomMessage.getMessageType(), roomMessage.getMessage(), RealmRoomMessage.getReplyMessageId(roomMessage), null);
                                }
                            }
                        }
                    }
                }, 1000 * j);
            } else {
                if (mMessages.get(j).messageID.equalsIgnoreCase(Long.toString(mSelectedMessageID))) {
                    RealmRoomMessage roomMessage = realm.where(RealmRoomMessage.class).equalTo(RealmRoomMessageFields.MESSAGE_ID, Long.parseLong(mMessages.get(j).messageID)).findFirst();
                    if (roomMessage != null) {
                        RealmRoom realmRoom = realm.where(RealmRoom.class).equalTo(RealmRoomFields.ID, roomMessage.getRoomId()).findFirst();
                        if (realmRoom != null) {
                            ProtoGlobal.Room.Type roomType = realmRoom.getType();
                            if (roomMessage.getAttachment() == null) {
                                G.chatSendMessageUtil.build(roomType, roomMessage.getRoomId(), roomMessage);
                            } else {
                                HelperUploadFile.startUploadTaskChat(roomMessage.getRoomId(), roomType, roomMessage.getAttachment().getLocalFilePath(), roomMessage.getMessageId(),
                                    roomMessage.getMessageType(), roomMessage.getMessage(), RealmRoomMessage.getReplyMessageId(roomMessage), null);
                            }
                        }
                    }
                    break;
                }
            }
        }

        realm.close();
    }

    @Override public void resendMessage() {
        resend(false);
    }

    @Override public void resendAllMessages() {
        resend(true);
    }
}

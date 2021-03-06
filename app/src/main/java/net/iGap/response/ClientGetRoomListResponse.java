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

import net.iGap.G;
import net.iGap.helper.HelperClientCondition;
import net.iGap.proto.ProtoClientGetRoomList;
import net.iGap.proto.ProtoError;
import net.iGap.request.RequestClientCondition;

import static net.iGap.realm.RealmRoom.putChatToDatabase;

public class ClientGetRoomListResponse extends MessageHandler {

    public int actionId;
    public Object message;
    public String identity;

    public ClientGetRoomListResponse(int actionId, Object protoClass, String identity) {
        super(actionId, protoClass, identity);

        this.message = protoClass;
        this.actionId = actionId;
        this.identity = identity;
    }

    @Override public void handler() {
        super.handler();
        final ProtoClientGetRoomList.ClientGetRoomListResponse.Builder clientGetRoomListResponse = (ProtoClientGetRoomList.ClientGetRoomListResponse.Builder) message;
        if (G.onClientGetRoomListResponse != null) {
            G.onClientGetRoomListResponse.onClientGetRoomList(clientGetRoomListResponse.getRoomsList(), clientGetRoomListResponse.getResponse(), Boolean.parseBoolean(identity));
        } else {

            new Thread(new Runnable() {
                @Override public void run() {
                    new RequestClientCondition().clientCondition(HelperClientCondition.computeClientCondition(null));
                    putChatToDatabase(clientGetRoomListResponse.getRoomsList(), false, false);
                }
            }).start();
        }
    }

    @Override public void timeOut() {
        super.timeOut();

        G.onClientGetRoomListResponse.onTimeout();
    }

    @Override public void error() {
        super.error();

        ProtoError.ErrorResponse.Builder errorResponse = (ProtoError.ErrorResponse.Builder) message;
        int majorCode = errorResponse.getMajorCode();
        int minorCode = errorResponse.getMinorCode();
        G.onClientGetRoomListResponse.onError(majorCode, minorCode);
    }
}



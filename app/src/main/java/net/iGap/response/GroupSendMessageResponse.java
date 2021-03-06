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

import net.iGap.helper.HelperMessageResponse;
import net.iGap.proto.ProtoGroupSendMessage;

import static net.iGap.realm.RealmRoomMessage.makeFailed;

public class GroupSendMessageResponse extends MessageHandler {

    public int actionId;
    public Object message;
    public String identity;

    public GroupSendMessageResponse(int actionId, Object protoClass, String identity) {
        super(actionId, protoClass, identity);

        this.message = protoClass;
        this.actionId = actionId;
        this.identity = identity;
    }

    @Override
    public void handler() {
        super.handler();
        final ProtoGroupSendMessage.GroupSendMessageResponse.Builder builder = (ProtoGroupSendMessage.GroupSendMessageResponse.Builder) message;
        HelperMessageResponse.handleMessage(builder.getRoomId(), builder.getRoomMessage(), builder.getResponse(), this.identity);
    }

    @Override
    public void error() {
        super.error();
        makeFailed(Long.parseLong(identity));
    }

    @Override
    public void timeOut() {
        super.timeOut();
    }
}

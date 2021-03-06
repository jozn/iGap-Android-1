/*
* This is the source code of iGap for Android
* It is licensed under GNU AGPL v3.0
* You should have received a copy of the license in this archive (see LICENSE).
* Copyright © 2017 , iGap - www.iGap.net
* iGap Messenger | Free, Fast and Secure instant messaging application
* The idea of the RooyeKhat Media Company - www.RooyeKhat.co
* All rights reserved.
*/

package net.iGap.interfaces;

import java.util.List;
import net.iGap.proto.ProtoGlobal;
import net.iGap.proto.ProtoResponse;

public interface OnClientGetRoomListResponse {
    void onClientGetRoomList(List<ProtoGlobal.Room> roomList, ProtoResponse.Response response, boolean fromLogin);

    void onError(int majorCode, int minorCode);

    void onTimeout();
}

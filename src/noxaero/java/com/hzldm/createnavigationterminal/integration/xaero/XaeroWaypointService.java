package com.hzldm.createnavigationterminal.integration.xaero;

import java.util.List;
import java.util.function.Consumer;

/** 无 Xaero 构建：路点列表恒为空。 */
public final class XaeroWaypointService {

    private XaeroWaypointService() {
    }

    public static void listWaypointsOnClient(Consumer<List<XaeroReflect.RawWp>> callback) {
        callback.accept(List.of());
    }
}

package com.hzldm.createnavigationterminal.integration.xaero;

/**
 * 无 Xaero 构建（{@code -PnoXaero}）下的空实现。
 *
 * <p>只保留调用方需要的类型，避免为「不读取任何第三方模组」的构建保留互操作代码。
 */
public final class XaeroReflect {

    private XaeroReflect() {
    }

    /** 路点快照（调用方构造用；此构建下不会产生任何真实数据） */
    public record RawWp(String name, int x, int y, int z) {
    }
}

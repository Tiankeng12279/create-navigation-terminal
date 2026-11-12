/**
 * 导航终端的公开读取 API。
 *
 * <p>约定：
 * <ul>
 *   <li><b>只读</b>：本包不提供任何修改终端状态的方法；需要写入请由玩家在界面内操作。</li>
 *   <li><b>不泄漏内部实现</b>：只暴露 {@code record} 与 Minecraft 原生类型，
 *       不出现 {@code NavigationTerminalBlockEntity} 等内部类。</li>
 *   <li><b>空安全</b>：非终端、未加载、跨维度一律返回 {@code Optional.empty()} 或哨兵值，
 *       不抛异常。</li>
 *   <li><b>线程</b>：{@code getXxx} 系列可在服务端主线程安全调用；客户端调用前请确认
 *       对应 {@code Level} 已加载。</li>
 *   <li><b>稳定性</b>：破坏性改动会升 minor 版本并在 CHANGELOG 标注。</li>
 * </ul>
 */
package com.hzldm.createnavigationterminal.api;

package com.yupi.aitravelplanner.agent;

// 死循环检测器：每次 run() 新建一个，记录本轮执行情况
// 用法：LoopGuard guard = new LoopGuard();
//       guard.recordThink(thinkResult);   // 每次 think 后调用
//       guard.recordTool(toolName);       // 每次 act 后调用
//       if (guard.isLooping()) { ... }    // 检测是否卡死
public class LoopGuard {

    // 阈值常量
    private static final int SAME_TOOL_THRESHOLD = 3;    // 连续 3 次同工具
    private static final int NO_PROGRESS_THRESHOLD = 5;  // 连续 5 次无进展

    // 状态字段
    private String lastToolName;       // 上次调用的工具名
    private int sameToolCount;          // 连续重复同一工具的次数
    private int noProgressCount;        // 连续 think 返回 false（无进展）的次数

    // 记录 think 结果（needTool=false 表示模型没调工具）
    public void recordThink(boolean needTool) {
        if (!needTool) {
            noProgressCount++;
        } else {
            noProgressCount = 0;  // 有工具调用，重置无进展计数
        }
    }

    // 记录 act 中调用的工具名
    public void recordTool(String toolName) {
        if (toolName.equals(lastToolName)) {
            sameToolCount++;
        } else {
            lastToolName = toolName;
            sameToolCount = 1;
        }
    }

    // 检测是否触发死循环
    public boolean isLooping() {
        return sameToolCount >= SAME_TOOL_THRESHOLD
                || noProgressCount >= NO_PROGRESS_THRESHOLD;
    }
}
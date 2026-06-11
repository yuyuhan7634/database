package org.example.cloud.demo.service;

/**
 * 住房分数计算工具
 * 职称基础分 + 家庭人口加成，供 ApplicationService 和 ResidentService 共用
 */
public final class ScoreCalculator {

    private ScoreCalculator() {}

    /**
     * 根据职称和家庭人口计算住房分数
     * @param title      职称（如 教授、讲师、工程师）
     * @param familySize 家庭人口数
     * @return 住房分数 = 职称基础分 + 家庭人口 × 10
     */
    public static int calculate(String title, Integer familySize) {
        int baseScore = 40;
        if (title != null) {
            if (title.contains("教授") || title.contains("处长")) baseScore = 100;
            else if (title.contains("科长") || title.contains("研究员")) baseScore = 80;
            else if (title.contains("讲师") || title.contains("工程师")) baseScore = 60;
            else if (title.contains("实验员") || title.contains("干事")) baseScore = 50;
        }
        return baseScore + (familySize != null ? familySize : 1) * 10;
    }
}

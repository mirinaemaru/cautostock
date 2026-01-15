package maru.trading.infra.config;

import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class P6SpyConfig {

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(PrettySqlFormat.class.getName());
    }

    public static class PrettySqlFormat implements MessageFormattingStrategy {

        @Override
        public String formatMessage(int connectionId, String now, long elapsed,
                                    String category, String prepared, String sql, String url) {
            if (sql == null || sql.isEmpty()) {
                return "";
            }

            String formattedSql = formatSql(sql);
            return String.format("\n/* %dms */ %s", elapsed, formattedSql);
        }

        private String formatSql(String sql) {
            sql = sql.trim().replaceAll("\\s+", " ");

            // SELECT 절의 컬럼 포맷팅 (콤마를 컬럼 앞으로) - 먼저 처리
            sql = formatSelectColumns(sql);

            // 키워드 앞에 줄바꿈 추가
            sql = sql.replaceAll("(?i)\\s+(FROM)\\b", "\nFROM");
            sql = sql.replaceAll("(?i)\\s+(WHERE)\\b", "\nWHERE");
            sql = sql.replaceAll("(?i)\\s+(AND)\\b", "\n   AND");
            sql = sql.replaceAll("(?i)\\s+(OR)\\b", "\n   OR");
            sql = sql.replaceAll("(?i)\\s+(ORDER BY)\\b", "\nORDER BY");
            sql = sql.replaceAll("(?i)\\s+(GROUP BY)\\b", "\nGROUP BY");
            sql = sql.replaceAll("(?i)\\s+(HAVING)\\b", "\nHAVING");
            sql = sql.replaceAll("(?i)\\s+(LEFT JOIN|RIGHT JOIN|INNER JOIN|JOIN)\\b", "\n$1");
            sql = sql.replaceAll("(?i)\\s+(ON)\\b", "\n     ON");
            sql = sql.replaceAll("(?i)\\s+(SET)\\b", "\nSET");
            sql = sql.replaceAll("(?i)\\s+(VALUES)\\b", "\nVALUES");

            return sql;
        }

        private String formatSelectColumns(String sql) {
            // SELECT ... FROM 사이의 컬럼들 찾기
            Pattern selectPattern = Pattern.compile("(?i)(select\\s+)(.*?)(\\s+from\\b)", Pattern.DOTALL);
            Matcher matcher = selectPattern.matcher(sql);

            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String selectKeyword = matcher.group(1);
                String columns = matcher.group(2);
                String fromKeyword = matcher.group(3);

                // 컬럼들을 분리하고 콤마를 앞으로 이동
                String[] cols = columns.split(",");
                StringBuilder formatted = new StringBuilder();

                for (int i = 0; i < cols.length; i++) {
                    String col = cols[i].trim();
                    if (i == 0) {
                        formatted.append(col);
                    } else {
                        formatted.append("\n     , ").append(col);
                    }
                }

                matcher.appendReplacement(result,
                    Matcher.quoteReplacement(selectKeyword + formatted.toString() + fromKeyword));
            }
            matcher.appendTail(result);

            return result.toString();
        }
    }
}

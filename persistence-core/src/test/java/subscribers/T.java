package subscribers;

import com.github.ixtf.japp.core.J;

import java.time.LocalDateTime;
import java.time.Month;

/**
 * @author jzb 2019-03-05
 */
public class T {
    public static void main(String[] args) {
        final LocalDateTime ldt = LocalDateTime.of(2019, Month.JUNE, 18, 5, 9, 27);
        // 1560841767000
        // 1560805767000
        System.out.println(J.date(ldt).getTime());
    }
}

package test;

import com.github.ixtf.Jpoi;
import lombok.SneakyThrows;

public class TestJpoi {
  @SneakyThrows
  public static void main(String[] args) {
    final var wb = Jpoi.wb("/home/jzb/Documents/test.xlsx");
    System.out.println(wb);
  }
}

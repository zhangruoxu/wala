package com.ibm.wala.examples.drivers;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Counter {
  private Date begin;
  private Date end;

  public Counter() {

  }

  public void begin() {
    if(begin == null) {
      begin = new Date();
    }
  }

  public void end() {
    if(end == null) {
      end = new Date();
    }
  }

  public long getMinute() {
    if(isValid()) {
      long time = end.getTime() - begin.getTime();
      return TimeUnit.MILLISECONDS.toMinutes(time);
    } else {
      return -1;
    }
  }

  public long getSecond() {
    if(isValid()) {
      long time = end.getTime() - begin.getTime();
      return TimeUnit.MILLISECONDS.toSeconds(time);
    } else {
      return -1;
    }

  }

  private boolean isValid() {
    return begin != null && end != null;
  }
}
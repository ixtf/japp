package com.github.ixtf.persistence;

import java.util.Date;

/** @author jzb 2019-02-28 */
public interface IEntityLoggable<T extends IOperator> extends IEntity {

  T getCreator();

  void setCreator(T operator);

  Date getCreateDateTime();

  void setCreateDateTime(Date date);

  T getModifier();

  void setModifier(T operator);

  Date getModifyDateTime();

  void setModifyDateTime(Date date);
}

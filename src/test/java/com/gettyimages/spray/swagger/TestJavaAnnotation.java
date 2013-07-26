package com.gettyimages.spray.swagger;

public @interface TestJavaAnnotation {
  boolean booleanValue() default false;
  String stringValue() default "";
  int intValue() default 0;
}
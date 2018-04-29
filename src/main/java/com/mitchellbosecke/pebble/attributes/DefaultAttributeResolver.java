package com.mitchellbosecke.pebble.attributes;

import com.mitchellbosecke.pebble.error.ClassAccessException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class DefaultAttributeResolver implements AttributeResolver {
  private final MemberCacheUtils memberCacheUtils = new MemberCacheUtils();

  @Override
  public ResolvedAttribute resolve(Object instance,
                                   Object attributeNameValue,
                                   Object[] argumentValues,
                                   boolean isStrictVariables,
                                   String filename,
                                   int lineNumber) {
    if (instance != null) {
      String attributeName = String.valueOf(attributeNameValue);

      Member member = memberCacheUtils.getMember(instance, attributeName);
      if (member == null) {
        if (argumentValues == null) {

          // first we check maps
          if (instance instanceof Map) {
            return MapResolver.INSTANCE.resolve(instance, attributeNameValue, argumentValues, isStrictVariables, filename, lineNumber);
          }

          // then we check arrays
          if (instance.getClass().isArray()) {
            return ArrayResolver.INSTANCE.resolve(instance, attributeNameValue, argumentValues, isStrictVariables, filename, lineNumber);
          }

          // then lists
          if (instance instanceof List) {
            return ListResolver.INSTANCE.resolve(instance, attributeNameValue, argumentValues, isStrictVariables, filename, lineNumber);
          }
        }

        member = memberCacheUtils.cacheMember(instance, attributeName, argumentValues);
      }

      if (member != null) {
        Member finalMember = member;
        return () -> this.invokeMember(instance, finalMember, argumentValues);
      }

      if (isStrictVariables && (attributeName.equals("class") || attributeName.equals("getClass"))) {
        throw new ClassAccessException(lineNumber, filename);
      }
    }
    return null;
  }

  /**
   * Invoke the "Member" that was found via reflection.
   */
  private Object invokeMember(Object object, Member member, Object[] argumentValues) {
    Object result = null;
    try {
      if (member instanceof Method) {
        result = ((Method) member).invoke(object, argumentValues);
      } else if (member instanceof Field) {
        result = ((Field) member).get(object);
      }

    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    return result;
  }
}
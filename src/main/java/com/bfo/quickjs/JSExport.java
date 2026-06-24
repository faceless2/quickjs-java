package com.bfo.quickjs;

import java.lang.annotation.*;

/**
 * <p>
 * An interface that can be used to create custom bindings. Any object implementing this
 * annotation can be passed into the JavaScript engine, where a proxy {@JSObject} will be
 * created that represents this object.
 * </p>
 *
 * <ul>
 *  <li>
 *   A class annotated with this property will be converted to a JSObject when sent to JS,
 *   and its fields and methods will be scanned for this annotation
 *  </li>
 *  <li>
 *   A field annotated with this property will be converted to a JS property
 *  </li>
 *  <li>
 *   A method annotated with this property and that sets "field" will be converted to the
 *   getter or setting for that property depending on the method signature: a method
 *   that takes no arguments and returns non-null becomes the getter. A method that takes
 *   one argument and returns null becomes the setter. Any other method signature is an
 *   error.
 *  </li>
 *  <li>
 *   A method annotated with this property and that does not set "field" will be converted
 *   to a JS Function
 *  </li>
 *  <li>
 *   Any properties (whether created from fields or getter/setter methods) may be annotated
 *   "hidden=true" to make them not enumerable in the obejct's key-set, or "deleteable=true"
 *   to allow them to be removed from the object and replaced by a regular (non-bound) property
 *  </li>
 *  <li>
 *   A property may only be declared once. If it is declared with a setter, it must also
 *   have a setter. A method may also only be declared once - although Java allows methods
 *   with different method-signatures to share the same name, this doesn't apply in JavaScript.
 *   Every exported method must have a different name.
 *  </li>
 *  <li>
 *   
 *  </li>
 * </ul>
 * <pre><code lang="java">
 *  @JSExport public class Test {    // exports must be accessible (ie public).
 *    
 *    @JSExport public String rwfield;                          // a writable field
 *    @JSExport public final String rofield;                    // a read-only field
 *    @JSExport(field="fieldname")  public String myname;       // exported as a different name
 *    @JSExport(hidden=true)  public int secret;                // not enumerable in Object
 *
 *    private String name;
 *    @JSExport(field="name") public String getName();          // getter for field "name"
 *    @JSExport(field="name") public void setName(String name); // setter for field "name"
 *
 *    @JSExport int add(int a, int b);                          // call as add(1, 2)
 *    @JSExport int addall(int... v);                           // call as addall(1, 2, 3, 4)
 *    @JSExport int addall2(int[] v);                           // call as addall2([1, 2, 3, 4])
 *    @JSExport String func1(Object... v);                      // call as func1(1, "2", ["etc"])
 *    @JSExport String func2(List<Object> v);                   // same as above
 *  }
 *
 * JSContext ctx = ...;
 * ctx.put("test", test);
 * Test test = new Test();
 * ctx.eval("test.name");       // Will return the value of test.getName()
 * </code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JSExport {
    /**
     * If this property is set on a field, or a method representing a field,
     * it will not be enumerated as part of the object properties. Defaults to false
     */
    boolean hidden() default false;

    /**
     * If this property is set on a field, or a method representing a field,
     * it may be deleted from the parent object. Defaults to false
     */
    boolean deleteable() default false;

    /**
     * If this property is set on a method that takes no arguments, it specifies
     * the name of the field which will have its value computed by calling this method.
     * If the value is set on a method that takes one argument, it specifies the
     * setter for the same property. A setter without a getter is an Exception
     */
    String field() default "";
}

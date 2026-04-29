/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.airlift.airstyle;

import org.junit.jupiter.api.Test;

import static io.airlift.airstyle.FormatterAssertions.assertFormatsOldToNew;

public class TestDeclarationFormatting
{
    @Test
    void testFormatterFixesUncommonDeclarationShapes()
    {
        String oldCode =
                """
                @SuppressWarnings({"unused","FieldCanBeLocal"})class Test{static{touch("static initializer");}{touch("instance initializer");}private transient volatile String state="initial";private native int nativeHash();void receiverParameter(Test this,String value){state=value;}@SafeVarargs final<T extends Number&Comparable<T>>T choose(T first,T...rest){return rest.length==0?first:rest[0];}interface Worker{private static String name(){return"worker";}private void validate(){if(name().isEmpty()){throw new IllegalStateException();}}default void run(){validate();}}private static void touch(String value){if(value.isBlank()){throw new IllegalArgumentException();}}}
                """;

        String newCode =
                """
                @SuppressWarnings({"unused", "FieldCanBeLocal"})
                class Test
                {
                    static {
                        touch("static initializer");
                    }

                    {
                        touch("instance initializer");
                    }

                    private transient volatile String state = "initial";

                    private native int nativeHash();

                    void receiverParameter(Test this, String value)
                    {
                        state = value;
                    }

                    @SafeVarargs
                    final <T extends Number & Comparable<T>> T choose(T first, T... rest)
                    {
                        return rest.length == 0 ? first : rest[0];
                    }

                    interface Worker
                    {
                        private static String name()
                        {
                            return "worker";
                        }

                        private void validate()
                        {
                            if (name().isEmpty()) {
                                throw new IllegalStateException();
                            }
                        }

                        default void run()
                        {
                            validate();
                        }
                    }

                    private static void touch(String value)
                    {
                        if (value.isBlank()) {
                            throw new IllegalArgumentException();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }
}

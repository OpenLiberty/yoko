package versioned;

import acme.Widget;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class SmallWidget extends NonSerializableSuper implements Widget {
    private static final long serialVersionUID = 1L;
    private String a = "a_v1";
    private transient String b = "b_v1";
    private String c = "c_v1";

    @Override
    public Widget validateAndReplace() {
        // check the v2 was transmitted correctly
        assertThat(a, endsWith("v2"));
        assertThat(b, nullValue());
        assertThat(c, nullValue());
        return new SmallWidget();
    }

    @Override
    public String toString() {
        return "WidgetImpl{" +
                "a='" + a + '\'' +
                ", b='" + b + '\'' +
                ", c='" + c + '\'' +
                '}';
    }
}

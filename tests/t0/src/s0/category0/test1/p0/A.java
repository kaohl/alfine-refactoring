package p0;

import org.lib.L0;
import p1.B;

public class A {

    L0 l;

    public A() {}

    public void setL0(L0 l) {
        this.l = l;
    }

    public void a() {
        B b = new B(new L0());
        b.b(this);
    }
}

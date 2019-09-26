package p1;

import org.lib.L0;
import p0.A;

public class B {

    private L0 l;

    public B(L0 l) {
        this.l = l;
    }

    public void b(A a) {
        a.setL0(l);
    }
}

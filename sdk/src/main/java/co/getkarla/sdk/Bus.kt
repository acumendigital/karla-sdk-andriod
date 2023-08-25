package co.getkarla.sdk

import com.squareup.otto.Bus;

class Bus {
    private var sBus: Bus? = null
    fun getBus(): Bus {
        if (sBus == null)
            sBus = Bus()
        return sBus as Bus
    }
}
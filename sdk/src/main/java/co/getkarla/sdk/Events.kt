package co.getkarla.sdk

val EventBus = Bus().getBus()
class Events {

    class NfcReadResult (result: String) {
        private var result: String = ""

        init {
            this.result = result
        }

        fun getResult(): String {
            return this.result
        }

    }

    class EmvReadResult (result: String) {
        private var result: String = ""

        init {
            this.result = result
        }

        fun getResult(): String {
            return this.result
        }

    }
}
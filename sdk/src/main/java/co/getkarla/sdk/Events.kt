package co.getkarla.sdk

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
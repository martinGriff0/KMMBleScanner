import SwiftUI
import shared
import os

struct ContentView: View {
	let greet = Greeting().greeting()
    
    var iosScanner: IOSBleScanner!
    
    
    func successFun(userData: String) {
        os_log("successFun")
        NSLog(userData)
    }
    
    init() {
        iosScanner = IOSBleScanner(onSuccess: successFun)
        os_log("INit")
    }

	var body: some View {
		Text(greet)
        Button("Scan", action: iosScanner.startScanner)
	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}

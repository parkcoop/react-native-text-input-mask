import Foundation
import InputMask

@objc(RNTextInputMask)
class TextInputMask: NSObject, RCTBridgeModule, MaskedTextFieldDelegateListener {
    static func moduleName() -> String {
        "TextInputMask"
    }
    
    @objc static func requiresMainQueueSetup() -> Bool {
        true
    }
    
    var methodQueue: DispatchQueue {
        bridge.uiManager.methodQueue
    }
    
    var bridge: RCTBridge!
    var masks: [String: MaskedTextFieldDelegate] = [:]
    
    @objc(mask:inputValue:autocomplete:resolver:rejecter:)
    func mask(mask: String, inputValue: String, autocomplete: Bool, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        let output = RNMask.maskValue(text: inputValue, format: mask, autcomplete: autocomplete)
        resolve(output)
    }
    
    @objc(unmask:inputValue:autocomplete:resolver:rejecter:)
    func unmask(mask: String, inputValue: String, autocomplete: Bool, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        let output = RNMask.unmaskValue(text: inputValue, format: mask, autocomplete: autocomplete)
        resolve(output)
    }
    
    @objc(setMask:mask:autocomplete:autoskip:)
    func setMask(reactNode: NSNumber, mask: String, autocomplete: Bool, autoskip: Bool) {
        bridge.uiManager.addUIBlock { (uiManager, viewRegistry) in
            DispatchQueue.main.async {
                guard let view = viewRegistry?[reactNode] as? RCTBaseTextInputView else { return }
                let textView = view.backedTextInputView as! RCTUITextField
                // let maskedDelegate = MaskedTextFieldDelegate(primaryFormat: mask, autocomplete: autocomplete, autoskip: autoskip) { (_, value, complete) in
                //     // trigger onChange directly to avoid trigger a second evaluation in native code (causes issue with some input masks like [00] {/} [00]
                //     let textField = textView as! UITextField
                //     view.onChange?([
                //         "text": textField.text,
                //         "target": view.reactTag,
                //         "eventCount": view.nativeEventCount,
                //     ])
                // }
                 let maskedDelegate = MaskedTextFieldDelegate(primaryFormat: mask, autocomplete: autocomplete, autoskip: autoskip, customNotations: [
                    Notation(character: "*", characterSet: CharacterSet(charactersIn: "*"), isOptional: true)
                  ]) { (_, value, complete) in
                    // trigger onChange directly to avoid trigger a second evaluation in native code (causes issue with some input masks like [00] {/} [00]
                    let textField = textView as! UITextField
                    view.onChange?([
                      "text": textField.text,
                      "target": view.reactTag,
                      "eventCount": view.nativeEventCount,
                    ])
                  }
                maskedDelegate.listener = textView.delegate as? UITextFieldDelegate & MaskedTextFieldDelegateListener
                let key = reactNode.stringValue
                self.masks[key] = maskedDelegate
                textView.delegate = self.masks[key]
            }
        }
    }
}

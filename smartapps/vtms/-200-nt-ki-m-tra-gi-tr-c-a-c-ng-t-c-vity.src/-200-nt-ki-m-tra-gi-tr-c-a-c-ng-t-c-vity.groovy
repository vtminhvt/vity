definition(
    name: "[200-nT] Kiểm tra giá trị của công tắc VITY",
    namespace: "VTMS",
    author: "Võ Thanh Minh",
    description: "Giao tiếp",
    category: "Safety & Security",
   iconUrl: "https://i.imgur.com/f73vWMD.png",
    iconX2Url: "https://i.imgur.com/f73vWMD.png",
    iconX3Url: "https://i.imgur.com/f73vWMD.png")


preferences {
     
    section("Chọn thông số cho kịch bản")
    {
    	input name:"sel",type:"enum", title:"Chọn ON để kích hoạt kịch bản", options: ["on","off"], defaultValue:"off"
    	input("swCC","capability.switch",title:"Chọn danh sách thiết bị, công tắc bạn muốn nhận thông báo trạng thái", multiple:true, required:true)
        input name:"txt1",type:"text", title:"Với thông báo khi Mở ",defaultValue:"Bật"
        input name:"txt2",type:"text", title:"Với thông báo khi Tắt ",defaultValue:"Tắt"
       
    }
}
def installed() 
{
	init()
}
def updated() 
{
	init()	
}
def init()
{ 
    subscribe(swCC,"switch",sw_CC)
   
}


def sw_CC(evt)

{
if (sel=="on")
	{
    // get the value of this event as an Integer
    // throws an exception if not convertable to Integer
    try {
        log.debug "The integerValue of this event is ${evt.integerValue}"
        log.debug "The integerValue of this event is an Integer: ${evt.integerValue instanceof Integer}"
    } catch (e) {
        log.debug "Trying to get the integerValue for ${evt.name} threw an exception: $e"
    }
	}

}
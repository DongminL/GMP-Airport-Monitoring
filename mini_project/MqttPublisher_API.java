package mini_project;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class MqttPublisher_API implements MqttCallback{	// implement callback 추가 & 필요한 메소드 정의
	static MqttClient sampleClient;	// MQTT Client 객체 선언
		
    public static void main(String[] args) {
    	MqttPublisher_API obj = new MqttPublisher_API();
    	obj.run();
    }
    
    public void run() {
		connectBroker();
    	    	
    	while(true) {    		
        	try {
        		String cong = get_congestion();	// 공공 API
            	String rain  = get_precipitation();	// 공공 API
            	String[] weather_data = get_weather();	// 공공 API
            	String wait = Integer.toString(get_waiting());	// 공공 API
            	 
            	publish_data("tmp", "{\"tmp\": "+weather_data[0]+"}");	// 온도 데이터 발행
            	publish_data("wsd", "{\"wsd\": "+weather_data[1]+"}");	// 풍속 데이터 발행
            	publish_data("rain", "{\"rain\": "+rain+"}");	// 1시간 강수량 데이터 발행
            	publish_data("cong", "{\"cong\": "+cong+"}");	// 공항 내 혼잡도 데이터 발행
            	publish_data("wait", "{\"wait\": "+wait+"}");	// 공항 소요시간 데이터 발행
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				try {
	    			sampleClient.disconnect();	    	        
	    		} catch (MqttException e) {    		
	    			e.printStackTrace();
	    		}
				e1.printStackTrace();
				System.out.println("Disconnected");
    	        System.exit(0);
			}        	    	
    	}
	}
    
    public void connectBroker() {
        String broker       = "tcp://127.0.0.1:1883"; // Broker Server IP
        String clientId     = "practice"; // Client ID
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            sampleClient = new MqttClient(broker, clientId, persistence); // Initialization of MQTT Client
            MqttConnectOptions connOpts = new MqttConnectOptions();	// 접속 시 접속의 옵션을 정의하는 객체 생성
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts);	// 브로커서버에 접속
            sampleClient.setCallback(this); // Callback option 추가
            System.out.println("Connected");
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
    
    public static void publish_data(String topic_input, String data) {
        String topic        = topic_input;
        int qos             = 0;
        try {
            System.out.println("Publishing message: "+data);
            sampleClient.publish(topic, data.getBytes(), qos, false);
            System.out.println("Message published");
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
    
    public static String get_precipitation() {
    	Date current = new Date(System.currentTimeMillis());
    	SimpleDateFormat d_format = new SimpleDateFormat("yyyyMMddHHmmss"); 
    	//System.out.println(d_format.format(current));
    	String date = d_format.format(current).substring(0,8);    	
    	String time = d_format.format(current).substring(8,10);    	
    	String url = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst" // https가 아닌 http 프로토콜을 통해 접근해야 함.
    			+ "?serviceKey=AxKFhxdDR5JI8Korb8UtgD7priD9TTCNbJNHcbKUwnMTheJsWzi8ShFK97gnDMbT7au9Oy2B2FcsbylWyvxDzQ%3D%3D"
    			+ "&pageNo=1"
    			+ "&numOfRows=1000"
    			+ "&dataType=XML"
    			+ "&base_date="+ date 
    			+ "&base_time=" + time + "00" 
    			+ "&nx=57"		// 김포국제공항 위치(X)
    			+ "&ny=126";	// 김포국제공항 위치(Y)
    	    	
		String rain = "";				
    	Document doc = null;
		
		// 2.파싱
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println(doc);
		
		Elements elements = doc.select("item");
		for (Element e : elements) {
			if (e.select("category").text().equals("RN1")) {
				rain = e.select("obsrValue").text();
			}
		}
		
    	return rain;
    }
    
    public static String[] get_weather() {
    	Date current = new Date(System.currentTimeMillis());
    	SimpleDateFormat d_format = new SimpleDateFormat("yyyyMMddHHmmss"); 
    	//System.out.println(d_format.format(current));
    	String date = d_format.format(current).substring(0,8);    	
    	String time = d_format.format(current).substring(8,10);    	
    	String url = "http://apis.data.go.kr/1360000/AirInfoService/getAirInfo" // https가 아닌 http 프로토콜을 통해 접근해야 함.
    			+ "?serviceKey=AxKFhxdDR5JI8Korb8UtgD7priD9TTCNbJNHcbKUwnMTheJsWzi8ShFK97gnDMbT7au9Oy2B2FcsbylWyvxDzQ%3D%3D"
    			+ "&pageNo=1"
    			+ "&numOfRows=1"
    			+ "&dataType=XML" 
    			+ "&fctm=" + date + time + "00" 
    			+ "&icaoCode=RKSS";	// 김포공항 코드
    	    	
		String tmp = "";
		String wsd = "";			
    	Document doc = null;
		
		// 2.파싱
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println(doc);
		
		Elements elements = doc.select("item");
		for (Element e : elements) {
			tmp = e.select("ta").text();
			wsd = e.select("ws").text();
		}
		
		String[] out = {tmp, wsd};
		
    	return out;
    }
    
    public static String get_congestion() {
    	String url = "http://api.odcloud.kr/api/"
    			+ "getAPRTPsgrCongestion/v1/aprtPsgrCongestion​"
    			+ "?page=1"
    			+ "&serviceKey=AxKFhxdDR5JI8Korb8UtgD7priD9TTCNbJNHcbKUwnMTheJsWzi8ShFK97gnDMbT7au9Oy2B2FcsbylWyvxDzQ=="
    			+ "&returnType=XML"
    			+ "&perPage=10"
    			+ "&cond%5BIATA_APCD%3A%3AEQ%5D=GMP";	// IATA 공항코드
    	
		String value = "";		
    	Document doc = null;
		
		// 2.파싱
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println(doc);
		
		Elements elements = doc.select("item");
		for (Element e : elements) {
			value = e.getElementsByAttributeValue("name", "CGDR_ALL_LVL").text();
		}
		
    	return value;
    }
    
    public static int get_waiting() {
    	String url = "http://api.odcloud.kr/api/getAPRTWaitTime/v1/aprtWaitTime"
    			+ "?page=1"
    			+ "&serviceKey=AxKFhxdDR5JI8Korb8UtgD7priD9TTCNbJNHcbKUwnMTheJsWzi8ShFK97gnDMbT7au9Oy2B2FcsbylWyvxDzQ%3D%3D"
    			+ "&returnType=XML"
    			+ "&perPage=10"
    			+ "&cond%5BIATA_APCD%3A%3AEQ%5D=GMP";	// IATA 공항코드 김포공항
    	
		String value = "";
		int wait = 0;
    	Document doc = null;
		
		// 2.파싱
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println(doc);
		
		Elements elements = doc.select("item");
		for (Element e : elements) {
			value = e.getElementsByAttributeValue("name", "STY_TCT_AVG_ALL").text();
		}
		
		wait = (int) Math.round(Integer.parseInt(value) / 60.0);	// 초단위 값을 분단위로 변경
		
    	return wait;
    }

	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		System.out.println("Connection Lost");
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub		
		
	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		// TODO Auto-generated method stub
	
	}    
}

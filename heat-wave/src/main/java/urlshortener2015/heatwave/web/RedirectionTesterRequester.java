package urlshortener2015.heatwave.web;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import urlshortener2015.heatwave.entities.ShortURL;
import urlshortener2015.heatwave.repository.ShortURLRepository;

@Service
public class RedirectionTesterRequester {
	
	/*
	 * Número máximo de redirecciones
	 */
	private static final int NUM_MAX_REDIRECCIONES = 5;
	
	/*
	 * Período de la tarea de actualizar las URLs
	 */
	private static final long T = 5*60; //5 minutos
	
	@Autowired
	protected ShortURLRepository shortURLRepository;
	
	/**
	 * Se comprueba periódicamente que las Urls no tienen más de
	 * 5 redirecciones.
	 */
	@Async
	@Scheduled(fixedRate=T*1000)
	public void testUrls(){
		
		Client client = ClientBuilder.newClient();
		Response response = null;
		
		// Se obtienen las URLs de la base de datos
		List<ShortURL> URLS = shortURLRepository.findAll();
		
		for(ShortURL url : URLS){
			String urlTarget = url.getTarget();
			for(int i=0; i<=NUM_MAX_REDIRECCIONES; i++){
				try{
					response = client.target(urlTarget).request().head();
				}catch(Exception e){
					shortURLRepository.mark(url, false);
					break;// Poner 404 en la base de datos
				}
				// Si el código es un 3xx
				if (response.getStatus() / 100 == 3){
					//Alcanzado el límite de redirecciones.
					if(i == NUM_MAX_REDIRECCIONES){
						shortURLRepository.mark(url, false);
						break;// Poner 404 en la base de datos
					}
					else{
						urlTarget = response.getLocation().toString();
					}
				}
				// Si el código no es un 3xx no es redirección
				else{
					// Si la URL estaba como no correcta en la base de datos se activa.
					shortURLRepository.mark(url, true);
					break;
				}
			}
		}
	}
}

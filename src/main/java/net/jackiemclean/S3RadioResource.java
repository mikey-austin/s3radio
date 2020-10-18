package net.jackiemclean;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("radio")
public class S3RadioResource {

    private final S3Radio radio;

    @Inject
    public S3RadioResource(S3Radio radio) {
        this.radio = radio;
    }

    @GET
    @Path("stations")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Station> getStations() {
        return radio.getStations();
    }
}

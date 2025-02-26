package net.jackiemclean;

import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

    @GET
    @Path("health")
    @Produces(MediaType.TEXT_PLAIN)
    public Response healthCheck() {
        boolean healthy = radio.getStations().stream().allMatch(Station::isMounted);
        return healthy ? Response.ok("OK").build() : Response.serverError().build();
    }

    @POST
    @Path("refresh")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Station> refresh() {
        radio.refresh();
        return radio.getStations();
    }

    @PUT
    @Path("standby/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Station standbyOn(@PathParam("name") String name) {
        Optional<Station> station = radio.getStation(name);
        station.ifPresent(s -> s.standbyOn());
        return station.orElse(null);
    }

    @DELETE
    @Path("standby/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Station standbyOff(@PathParam("name") String name) {
        Optional<Station> station = radio.getStation(name);
        station.ifPresent(s -> s.standbyOff());
        return station.orElse(null);
    }
}

package company.tothepoint.controller;

import company.tothepoint.model.bediende.*;
import company.tothepoint.repository.BediendeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/bediendes")
public class BediendeController {
    private static final Logger LOG = LoggerFactory.getLogger(BediendeController.class);
    @Autowired
    private BediendeRepository bediendeRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private String exchangeName;

    @Autowired
    private String routingKey;


    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Bediende>> getAllBediendes() {
        LOG.debug("GET /bediendes getAllBediendes() called!");
        return new ResponseEntity<>(bediendeRepository.findAll(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    public ResponseEntity<Bediende> getBediende(@PathVariable("id") String id) {
        LOG.debug("GET /bediendes/"+id+" getBediende("+id+") called!");
        Optional<Bediende> bediendeOption = Optional.ofNullable(bediendeRepository.findOne(id));

        return bediendeOption.map(bediende->
            new ResponseEntity<>(bediende, HttpStatus.OK)
        ).orElse(
            new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Bediende> createBediende(@RequestBody Bediende bediende) {
        LOG.debug("POST /bediendes createBediende(..) called!");
        Bediende createdBediende = bediendeRepository.save(bediende);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, new BediendeCreatedNotification("bediendeCreated", createdBediende));
        return new ResponseEntity<>(createdBediende, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{id}")
    public ResponseEntity<Bediende> updateBediende(@PathVariable("id") String id, @RequestBody Bediende bediende) {
        LOG.debug("PUT /bediendes/"+id+" updateBediende("+id+", ..) called!");
        Optional<Bediende> existingBediende = Optional.ofNullable(bediendeRepository.findOne(id));

        return existingBediende.map(bu ->
            {
                bediende.setId(id);
                rabbitTemplate.convertAndSend(exchangeName, routingKey, new BediendeUpdatedNotification("bediendeUpdated", bediende));
                return new ResponseEntity<>(bediendeRepository.save(bediende), HttpStatus.OK);
            }
        ).orElse(
            new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
    public ResponseEntity<Bediende> deleteBediende(@PathVariable("id") String id) {
        LOG.debug("DELETE /bediendes/"+id+" deleteBediende("+id+") called!");
        if (bediendeRepository.exists(id)) {
            bediendeRepository.delete(id);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, new BediendeDeletedNotification("bediendeDeleted", id));
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}

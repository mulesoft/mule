/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.testmodels.artistregistry;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebParam;

public class ArtistRegistry implements ArtistRegistryWebServices
{
    private List<Artist> artists;

    public ArtistRegistry()
    {
        initMockArtists();
    }

    @Override
    public void addArtist(@WebParam(partName = "arg0", name = "arg0") Artist arg0)
    {
        artists.add(arg0);
    }

    @Override
    public ArtistArray getAll(@WebParam(partName = "pageSize", name = "pageSize") int pageSize, @WebParam(partName = "pageNumber", name = "pageNumber") int pageNumber)
    {
        ArtistArray artistArray = new ArtistArray();
        List<Artist> artistList = artistArray.getItem();
        artistList.addAll(artists);
        return artistArray;
    }

    private void initMockArtists()
    {
        artists = new ArrayList<Artist>();
        Artist author = new Artist();
        author.setArtType(ArtType.AUTHOR);
        author.setFamousWorks("Hamlet");
        author.setFirstName("William");
        author.setLastName("Shakespeare");
        artists.add(author);

        Artist actor = new Artist();
        actor.setArtType(ArtType.ACTOR);
        actor.setFamousWorks("Mission Impossible");
        actor.setFirstName("Tom");
        actor.setLastName("Cruise");
        artists.add(actor);
    }
}

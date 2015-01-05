/*
 * Copyright (c) 2013 LDBC
 * Linked Data Benchmark Council (http://ldbc.eu)
 *
 * This file is part of ldbc_socialnet_dbgen.
 *
 * ldbc_socialnet_dbgen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ldbc_socialnet_dbgen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ldbc_socialnet_dbgen.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2011 OpenLink Software <bdsmt@openlinksw.com>
 * All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation;  only Version 2 of the License dated
 * June 1991.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */


package ldbc.snb.datagen.serializer.snb.interactive;

import ldbc.snb.datagen.dictionary.BrowserDictionary;
import ldbc.snb.datagen.dictionary.LanguageDictionary;
import ldbc.snb.datagen.dictionary.PlaceDictionary;
import ldbc.snb.datagen.generator.DatagenParams;
import ldbc.snb.datagen.generator.DateGenerator;
import ldbc.snb.datagen.objects.Knows;
import ldbc.snb.datagen.objects.Person;
import ldbc.snb.datagen.objects.StudyAt;
import ldbc.snb.datagen.objects.WorkAt;
import ldbc.snb.datagen.serializer.HDFSCSVWriter;
import ldbc.snb.datagen.serializer.PersonSerializer;
import org.apache.hadoop.conf.Configuration;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import ldbc.snb.datagen.dictionary.Dictionaries;

public class CSVPersonSerializer extends PersonSerializer {

    private HDFSCSVWriter [] writers;

    private enum FileNames {
        PERSON ("person"),
        PERSON_SPEAKS_LANGUAGE ("person_speaks_language"),
        PERSON_HAS_EMAIL ("person_email_emailaddress"),
        PERSON_LOCATED_IN_PLACE ("person_isLocatedIn_place"),
        PERSON_HAS_INTEREST_TAG ("person_hasInterest_tag"),
        PERSON_WORK_AT ("person_workAt_organization"),
        PERSON_STUDY_AT ("person_studyAt_organization"),
        PERSON_KNOWS_PERSON("person_knows_person");

        private final String name;

        private FileNames( String name ) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
    }

    public CSVPersonSerializer() {
    }

    public void initialize(Configuration conf, int reducerId) {
        int numFiles = FileNames.values().length;
        writers = new HDFSCSVWriter[numFiles];
        for( int i = 0; i < numFiles; ++i) {
            writers[i] = new HDFSCSVWriter(conf.get("ldbc.snb.datagen.serializer.socialNetworkDir"),FileNames.values()[i].toString()+"_"+reducerId,conf.getInt("ldbc.snb.datagen.numPartitions",1),conf.getBoolean("ldbc.snb.datagen.serializer.compressed",false),"|");
        }
    }

    @Override
    public void close() {
        int numFiles = FileNames.values().length;
        for(int i = 0; i < numFiles; ++i) {
            writers[i].close();
        }
    }

    @Override
    protected void serialize(Person p) {

        ArrayList<String> arguments = new ArrayList<String>();

        arguments.add(Long.toString(p.accountId()));
        arguments.add(p.firstName());
        arguments.add(p.lastName());
        if(p.gender() == 1) {
            arguments.add("male");
        } else {
            arguments.add("female");
        }

        String dateString = Dictionaries.dates.formatDate(p.birthDay());
        arguments.add(dateString);

        dateString = Dictionaries.dates.formatDateDetail(p.creationDate());
        arguments.add(dateString);
        arguments.add(p.ipAddress().toString());
        arguments.add(Dictionaries.browsers.getName(p.browserId()));
        writers[FileNames.PERSON.ordinal()].writeEntry(arguments);

        ArrayList<Integer> languages = p.languages();
        for (int i = 0; i < languages.size(); i++) {
            arguments.clear();
            arguments.add(Long.toString(p.accountId()));
            arguments.add(Dictionaries.languages.getLanguageName(languages.get(i)));
            writers[FileNames.PERSON_SPEAKS_LANGUAGE.ordinal()].writeEntry(arguments);
        }

        Iterator<String> itString = p.emails().iterator();
        while (itString.hasNext()) {
            arguments.clear();
            String email = itString.next();
            arguments.add(Long.toString(p.accountId()));
            arguments.add(email);
            writers[FileNames.PERSON_HAS_EMAIL.ordinal()].writeEntry(arguments);
        }

        arguments.clear();
        arguments.add(Long.toString(p.accountId()));
        arguments.add(Integer.toString(p.cityId()));
        writers[FileNames.PERSON_LOCATED_IN_PLACE.ordinal()].writeEntry(arguments);

        Iterator<Integer> itInteger = p.interests().iterator();
        while (itInteger.hasNext()) {
            arguments.clear();
            Integer interestIdx = itInteger.next();
            arguments.add(Long.toString(p.accountId()));
            arguments.add(Integer.toString(interestIdx));
            writers[FileNames.PERSON_HAS_INTEREST_TAG.ordinal()].writeEntry(arguments);
        }
    }

    @Override
    protected void serialize(StudyAt studyAt) {
        ArrayList<String> arguments = new ArrayList<String>();
        String dateString = Dictionaries.dates.formatYear(studyAt.year);
        arguments.add(Long.toString(studyAt.user));
        arguments.add(Long.toString(studyAt.university));
        arguments.add(dateString);
        writers[FileNames.PERSON_STUDY_AT.ordinal()].writeEntry(arguments);
    }

    @Override
    protected void serialize(WorkAt workAt) {
        ArrayList<String> arguments = new ArrayList<String>();
        String dateString = Dictionaries.dates.formatYear(workAt.year);
        arguments.add(Long.toString(workAt.user));
        arguments.add(Long.toString(workAt.company));
        arguments.add(dateString);
        writers[FileNames.PERSON_WORK_AT.ordinal()].writeEntry(arguments);
    }

    protected void serialize(Knows knows) {
        ArrayList<String> arguments = new ArrayList<String>();
        String dateString = Dictionaries.dates.formatDateDetail(knows.creationDate());
        arguments.add(Long.toString(knows.from().creationDate()));
        arguments.add(Long.toString(knows.to().creationDate()));
        arguments.add(dateString);
        writers[FileNames.PERSON_KNOWS_PERSON.ordinal()].writeEntry(arguments);
    }
}

import React from 'react';
import artists from '../data/music.json';
import '../css/Music.css';

const sortKey = (name) => name.replace(/^The\s+/i, '');

const parseDate = (dateStr) => {
  const [month, day, year] = dateStr.split('/').map(Number);
  return new Date(year, month - 1, day);
};

const parseDateParts = (dateStr) => {
  const MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
  const [month, day, year] = dateStr.split('/').map(Number);
  return { month: MONTHS[month - 1], day, year };
};

const getExpectedYear = (dateStr) => dateStr.split('/')[2];

const UpcomingCard = ({ artist }) => {
  const { month, day, year } = parseDateParts(artist.nextRelease);
  return (
    <div className="upcomingCard">
      <div className="upcomingCardDate">
        <div className="upcomingCardMonth">{month}</div>
        <div className="upcomingCardDay">{day}</div>
        <div className="upcomingCardYear">{year}</div>
      </div>
      <div className="upcomingCardDivider" />
      <div className="upcomingCardInfo">
        <div className="upcomingCardArtist">
          {artist.name}
          {artist.incompleteCollection && (
            <span className="incompleteDot" title="Incomplete collection"> ●</span>
          )}
        </div>
        {artist.albumTitle && (
          <div className="upcomingCardAlbum">{artist.albumTitle}</div>
        )}
      </div>
    </div>
  );
};

const ArtistRow = ({ artist, dateDisplay }) => (
  <div className="musicRow">
    <span className="musicArtistName">
      {artist.name}
      {artist.incompleteCollection && (
        <span className="incompleteDot" title="Incomplete collection"> ●</span>
      )}
    </span>
    <span className="musicAlbumTitle">
      {artist.albumTitle || '—'}
      {artist.notes && (
        <span className="musicNotes"> ({artist.notes})</span>
      )}
    </span>
    <span className="musicDate">{dateDisplay}</span>
    <span className="musicLastRelease">{artist.lastRelease || '—'}</span>
  </div>
);

const TableHeader = ({ col3Label }) => (
  <div className="musicTableHeader">
    <span>Artist</span>
    <span>Album</span>
    <span>{col3Label}</span>
    <span>Last Release</span>
  </div>
);

const Music = () => {
  const upcoming = artists
    .filter((a) => a.nextRelease && a.confirmed)
    .sort((a, b) => parseDate(a.nextRelease) - parseDate(b.nextRelease));

  const expected = artists
    .filter((a) => a.nextRelease && !a.confirmed)
    .sort((a, b) => {
      const yearA = parseInt(getExpectedYear(a.nextRelease));
      const yearB = parseInt(getExpectedYear(b.nextRelease));
      if (yearA !== yearB) return yearA - yearB;
      return sortKey(a.name).localeCompare(sortKey(b.name));
    });

  const watching = artists
    .filter((a) => !a.nextRelease)
    .sort((a, b) => {
      const yearA = a.lastRelease ? parseInt(a.lastRelease) : 0;
      const yearB = b.lastRelease ? parseInt(b.lastRelease) : 0;
      if (yearB !== yearA) return yearB - yearA;
      return sortKey(a.name).localeCompare(sortKey(b.name));
    });

  return (
    <div className="musicPage">
      <div className="musicHeader">
        <h1 className="musicTitle">Release Schedule</h1>
        <p className="musicLegend">
          <span className="incompleteDot">●</span> incomplete collection
        </p>
      </div>

      {upcoming.length > 0 && (
        <section className="musicSection">
          <h2 className="musicSectionTitle">Upcoming</h2>
          <div className="upcomingGrid">
            {upcoming.map((artist) => (
              <UpcomingCard key={artist.name} artist={artist} />
            ))}
          </div>
        </section>
      )}

      {expected.length > 0 && (
        <section className="musicSection">
          <h2 className="musicSectionTitle">Expected</h2>
          <TableHeader col3Label="Year" />
          {expected.map((artist) => (
            <ArtistRow
              key={artist.name}
              artist={artist}
              dateDisplay={`~${getExpectedYear(artist.nextRelease)}`}
            />
          ))}
        </section>
      )}

      {watching.length > 0 && (
        <section className="musicSection">
          <h2 className="musicSectionTitle">Watching</h2>
          <TableHeader col3Label="" />
          {watching.map((artist) => (
            <ArtistRow
              key={artist.name}
              artist={artist}
              dateDisplay="—"
            />
          ))}
        </section>
      )}
    </div>
  );
};

export default Music;

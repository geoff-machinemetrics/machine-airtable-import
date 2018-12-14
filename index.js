const request = require('request-promise');
const program = require('commander');
const { writeFile } = require('fs');
const { promisify } = require('util');

const asyncWriteFile = promisify(writeFile);

const retrieveMachines = (domain, accessToken, offset, limit) => {
  return request({
    uri: `${domain}/dmz/airtable/import-machines`,
    method: 'POST',
    json: { offset, limit },
    headers: {
      'DMZ-Access-Token': accessToken,
    },
  });
};

const importMachines = async (accumulatedMachines, domain, accessToken, options = {}) => {
  const { offset, limit } = options;
  console.log(`FETCHING records between ${offset} and ${offset + limit}`);
  const { size, results: machines } = await retrieveMachines(domain, accessToken, offset, limit);
  if (size === 0) {
    console.log(`DONE IMPORTING ${accumulatedMachines.length} machines`);
    return accumulatedMachines;
  }

  const nextAccumulatedMachines = accumulatedMachines.concat(machines);
  return importMachines(nextAccumulatedMachines, domain, accessToken, {
    offset: offset + limit,
    limit,
  });
};

const main = async () => {
  program
    .option('-u, --urlBase <urlBase>', 'Url base')
    .option('-a, --access-token <accessToken>', 'Access Token')
    .option('-o, --offset [offset]', 'Offset', 0)
    .option('-l, --limit [limit]', 'Limit', 100)
    .option('-f, --result-file [resultFile]', 'File to write result to', './import-results.json')
    .parse(process.argv);

  if (!program.urlBase || !program.accessToken) {
    throw new Error('Both url and accessToken are required');
  }

  const options = {
    offset: program.offset,
    limit: program.limit,
  };

  const machines = await importMachines([], program.urlBase, program.accessToken, options);

  await asyncWriteFile(program.resultFile, JSON.stringify(machines), 'utf8');
  console.log(`Wrote results to ${program.resultFile}`);
};

main();

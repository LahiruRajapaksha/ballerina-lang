package io.ballerina.projects.internal.repositories;

import io.ballerina.projects.DependencyGraph;
import io.ballerina.projects.JvmTarget;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.ProjectException;
import io.ballerina.projects.SemanticVersion;
import io.ballerina.projects.Settings;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.PackageRepository;
import io.ballerina.projects.environment.ResolutionRequest;
import io.ballerina.projects.environment.ResolutionResponse;
import io.ballerina.projects.environment.ResolutionResponseDescriptor;
import io.ballerina.projects.internal.ImportModuleRequest;
import io.ballerina.projects.internal.ImportModuleResponse;
import org.ballerinalang.central.client.CentralAPIClient;
import org.ballerinalang.central.client.exceptions.CentralClientException;
import org.ballerinalang.central.client.exceptions.ConnectionErrorException;
import org.ballerinalang.central.client.model.PackageResolutionRequest;
import org.ballerinalang.central.client.model.PackageResolutionResponse;
import org.wso2.ballerinalang.util.RepoUtils;

import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.ballerina.projects.DependencyGraph.DependencyGraphBuilder.getBuilder;
import static io.ballerina.projects.util.ProjectUtils.getAccessTokenOfCLI;
import static io.ballerina.projects.util.ProjectUtils.initializeProxy;
import static org.wso2.ballerinalang.programfile.ProgramFileConstants.SUPPORTED_PLATFORMS;

/**
 * This class represents the remote package repository.
 *
 * @since 2.0.0
 */
public class RemotePackageRepository implements PackageRepository {

    private FileSystemRepository fileSystemRepo;
    private CentralAPIClient client;

    public RemotePackageRepository(FileSystemRepository fileSystemRepo, CentralAPIClient client) {
        this.fileSystemRepo = fileSystemRepo;
        this.client = client;
    }

    public static RemotePackageRepository from(Environment environment, Path cacheDirectory, String repoUrl,
                                               Settings settings) {
        if (Files.notExists(cacheDirectory)) {
            throw new ProjectException("cache directory does not exists: " + cacheDirectory);
        }
        String ballerinaShortVersion = RepoUtils.getBallerinaShortVersion();
        FileSystemRepository fileSystemRepository = new FileSystemRepository(
                environment, cacheDirectory, ballerinaShortVersion);
        Proxy proxy = initializeProxy(settings.getProxy());
        CentralAPIClient client = new CentralAPIClient(repoUrl, proxy, getAccessTokenOfCLI(settings));

        return new RemotePackageRepository(fileSystemRepository, client);
    }

    public static RemotePackageRepository from(Environment environment, Path cacheDirectory, Settings settings) {
        String repoUrl = RepoUtils.getRemoteRepoURL();
        if ("".equals(repoUrl)) {
            throw new ProjectException("remote repo url is empty");
        }

        return from(environment, cacheDirectory, repoUrl, settings);
    }

    @Override
    public Optional<Package> getPackage(ResolutionRequest resolutionRequest) {
        // Avoid resolving from remote repository for lang repo tests
        String langRepoBuild = System.getProperty("LANG_REPO_BUILD");
        if (langRepoBuild != null) {
            return Optional.empty();
        }

        // Check if the package is in cache
        Optional<Package> cachedPackage = this.fileSystemRepo.getPackage(resolutionRequest);
        if (cachedPackage.isPresent()) {
            return cachedPackage;
        }

        String packageName = resolutionRequest.packageName().value();
        String orgName = resolutionRequest.orgName().value();
        String version = resolutionRequest.version().isPresent() ? resolutionRequest.version().get().toString() : null;

        Path packagePathInBalaCache = this.fileSystemRepo.bala.resolve(orgName).resolve(packageName);

        // If environment is online pull from central
        if (!resolutionRequest.offline()) {
            for (String supportedPlatform : SUPPORTED_PLATFORMS) {
                try {
                    this.client.pullPackage(orgName, packageName, version, packagePathInBalaCache, supportedPlatform,
                            RepoUtils.getBallerinaVersion(), true);
                } catch (CentralClientException e) {
                    // ignore when get package fail
                }
            }
        }

        return this.fileSystemRepo.getPackage(resolutionRequest);
    }

    @Override
    public List<PackageVersion> getPackageVersions(ResolutionRequest resolutionRequest) {
        String langRepoBuild = System.getProperty("LANG_REPO_BUILD");
        if (langRepoBuild != null) {
            return Collections.emptyList();
        }
        String orgName = resolutionRequest.orgName().value();
        String packageName = resolutionRequest.packageName().value();

        // First, Get local versions
        Set<PackageVersion> packageVersions = new HashSet<>(fileSystemRepo.getPackageVersions(resolutionRequest));

        // If the resolution request specifies to resolve offline, we return the local version
        if (resolutionRequest.offline()) {
            return new ArrayList<>(packageVersions);
        }

        try {
            for (String version : this.client.getPackageVersions(orgName, packageName, JvmTarget.JAVA_11.code(),
                    RepoUtils.getBallerinaVersion())) {
                packageVersions.add(PackageVersion.from(version));
            }
        } catch (ConnectionErrorException e) {
            // ignore connect to remote repo failure
            return new ArrayList<>(packageVersions);
        } catch (CentralClientException e) {
            throw new ProjectException(e.getMessage());
        }
        return new ArrayList<>(packageVersions);
    }

    @Override
    public Map<String, List<String>> getPackages() {
        // We only return locally cached packages
        return fileSystemRepo.getPackages();
    }

    @Override
    public List<ImportModuleResponse> resolvePackageNames(List<ImportModuleRequest> importModuleRequests) {
        List<ImportModuleResponse> filesystem = fileSystemRepo.resolvePackageNames(importModuleRequests);
        return filesystem;
    }

    @Override
    public List<ResolutionResponseDescriptor> resolveDependencyVersions(List<ResolutionRequest> packageLoadRequests) {
        // Resolve all the requests locally
        List<ResolutionResponseDescriptor> filesystem = fileSystemRepo.resolveDependencyVersions(packageLoadRequests);

        // Filter out the requests that can be resolved online
        List<ResolutionRequest> online = packageLoadRequests.stream()
                .filter(i -> {
                    return i.offline() == false;
                }).collect(Collectors.toList());
        // Resolve the requests from remote repository
        try {
            List<ResolutionResponseDescriptor> remoteResolution;
            if (online.size() > 0) {
                PackageResolutionRequest packageResolutionRequest = toPackageResolutionRequest(online);
                PackageResolutionResponse packageResolutionResponse = client.resolveDependencies(
                        packageResolutionRequest, JvmTarget.JAVA_11.code(),
                        RepoUtils.getBallerinaVersion(), true);

                remoteResolution = fromPackageResolutionResponse(packageLoadRequests, packageResolutionResponse);
                // Merge central requests and local requests
                // Here we will pick the latest package from remote or local
                return mergeResolution(remoteResolution, filesystem);
            }
        } catch (ConnectionErrorException e) {
            // ignore connect to remote repo failure
            // TODO we need to add diagnostics for resolution errors
        } catch (CentralClientException e) {
            throw new ProjectException(e.getMessage());
        }
        // If any issue we will return filesystem results
        return filesystem;
    }

    private List<ResolutionResponseDescriptor> mergeResolution(
            List<ResolutionResponseDescriptor> remoteResolution,
            List<ResolutionResponseDescriptor> filesystem) {
        // accumilate the result to a new list
        List<ResolutionResponseDescriptor> mergedResults = new ArrayList<>();
        // Iterate resolutions in original list
        for (ResolutionResponseDescriptor remote : remoteResolution) {
            Optional<ResolutionResponseDescriptor> local;
            local = filesystem.stream().filter(d -> {
                return d.packageLoadRequest().equals(remote.packageLoadRequest());
            }).findFirst();

            // if a local dependency is not found this case cannot be true
            if (local.isEmpty()) {
                // Both remote and local should have the same number of results
                throw new AssertionError("Un resolved local dependency");
            }
            ResolutionResponseDescriptor localDescriptor = local.get();
            // if local is unresolved add remote
            if (localDescriptor.resolutionStatus() == ResolutionResponse.ResolutionStatus.UNRESOLVED) {
                mergedResults.add(remote);
                continue;
            }
            if (remote.resolutionStatus() == ResolutionResponse.ResolutionStatus.UNRESOLVED) {
                mergedResults.add(localDescriptor);
                continue;
            }
            // pick the latest of both
            SemanticVersion.VersionCompatibilityResult versionCompatibilityResult =
                    remote.resolvedDescriptor().get().version().compareTo(
                            localDescriptor.resolvedDescriptor().get().version());

            if (versionCompatibilityResult.equals(SemanticVersion.VersionCompatibilityResult.GREATER_THAN)) {
                mergedResults.add(remote);
            } else {
                mergedResults.add(localDescriptor);
            }
        }
        return mergedResults;
    }

    private List<ResolutionResponseDescriptor> fromPackageResolutionResponse(
            List<ResolutionRequest> packageLoadRequests, PackageResolutionResponse packageResolutionResponse) {
        // List<PackageResolutionResponse.Package> resolved = packageResolutionResponse.resolved();
        List<ResolutionResponseDescriptor> response = new ArrayList<>();
        for (ResolutionRequest resolutionRequest : packageLoadRequests) {
            // find response from server
            // checked in resolved group
            Optional<PackageResolutionResponse.Package> match = packageResolutionResponse.resolved().stream()
                    .filter(p -> p.name().equals(resolutionRequest.packageName().value()) &&
                            p.orgName().equals(resolutionRequest.orgName().value())).findFirst();
            // If we found a match we will add it to response
            if (match.isPresent()) {
                PackageVersion version = PackageVersion.from(match.get().version());
                DependencyGraph<PackageDescriptor> dependancies = createPackageDependencyGraph(match.get());
                PackageDescriptor packageDescriptor = PackageDescriptor.from(resolutionRequest.orgName(),
                        resolutionRequest.packageName(),
                        version);
                ResolutionResponseDescriptor responseDescriptor = ResolutionResponseDescriptor.from(resolutionRequest,
                        packageDescriptor,
                        dependancies);
                response.add(responseDescriptor);
            } else {
                // If the package is not in resolved we assume the package is unresolved
                response.add(ResolutionResponseDescriptor.from(resolutionRequest));
            }
        }
        return response;
    }

    private static DependencyGraph<PackageDescriptor> createPackageDependencyGraph(
            PackageResolutionResponse.Package aPackage) {
        DependencyGraph.DependencyGraphBuilder<PackageDescriptor> graphBuilder = getBuilder();

        for (PackageResolutionResponse.Package dependency : aPackage.dependencies()) {
            PackageDescriptor pkg = PackageDescriptor.from(PackageOrg.from(dependency.orgName()),
                    PackageName.from(dependency.name()), PackageVersion.from(dependency.version()));
            Set<PackageDescriptor> dependentPackages = new HashSet<>();
            for (PackageResolutionResponse.Package dependencyPkg : dependency.dependencies()) {
                dependentPackages.add(PackageDescriptor.from(PackageOrg.from(dependencyPkg.orgName()),
                        PackageName.from(dependencyPkg.name()),
                        PackageVersion.from(dependencyPkg.version())));
            }
            graphBuilder.addDependencies(pkg, dependentPackages);
        }

        return graphBuilder.build();
    }


    private PackageResolutionRequest toPackageResolutionRequest(List<ResolutionRequest> resolutionRequests) {
        PackageResolutionRequest packageResolutionRequest = new PackageResolutionRequest();
        for (ResolutionRequest resolutionRequest : resolutionRequests) {
            PackageResolutionRequest.Mode mode = PackageResolutionRequest.Mode.HARD;
            switch (resolutionRequest.packageLockingMode()) {
                case HARD:
                    mode = PackageResolutionRequest.Mode.HARD;
                    break;
                case MEDIUM:
                    mode = PackageResolutionRequest.Mode.MEDIUM;
                    break;
                case SOFT:
                    mode = PackageResolutionRequest.Mode.SOFT;
                    break;
            }
            packageResolutionRequest.addPackage(resolutionRequest.orgName().value(),
                    resolutionRequest.packageName().value(),
                    resolutionRequest.version().get().value().toString(),
                    mode);
        }
        return packageResolutionRequest;
    }
}
